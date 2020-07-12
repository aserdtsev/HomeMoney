package ru.serdtsev.homemoney.moneyoper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.serdtsev.homemoney.account.AccountRepository;
import ru.serdtsev.homemoney.account.BalanceRepository;
import ru.serdtsev.homemoney.account.CategoryRepository;
import ru.serdtsev.homemoney.account.model.*;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.moneyoper.model.*;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MoneyOperResourceTest {
  private DataSource dataSource = mock(DataSource.class);
  private MoneyOperResource mtRes;
  private AccountRepository accountRepo = mock(AccountRepository.class);
  private MoneyOperService moneyOperService = mock(MoneyOperService.class);
  private BalanceSheetRepository balanceSheetRepo = mock(BalanceSheetRepository.class);
  private BalanceRepository balanceRepo = mock(BalanceRepository.class);
  private MoneyOperRepo moneyOperRepo = mock(MoneyOperRepo.class);
  private LabelRepository labelRepo = mock(LabelRepository.class);
  private MoneyOperItemRepo moneyOperItemRepo = mock(MoneyOperItemRepo.class);
  private CategoryRepository categoryRepo = mock(CategoryRepository.class);
  private BalanceSheet balanceSheet = BalanceSheet.Companion.newInstance();
  private Balance cash;
  private Balance currentAccount;

  public MoneyOperResourceTest() {
    this.mtRes = new MoneyOperResource(moneyOperService, balanceSheetRepo, accountRepo, balanceRepo, moneyOperRepo, labelRepo,
        moneyOperItemRepo, categoryRepo);
    when(accountRepo.findById(balanceSheet.getUncatCosts().getId())).thenReturn(Optional.of(balanceSheet.getUncatCosts()));
    when(accountRepo.findById(balanceSheet.getUncatIncome().getId())).thenReturn(Optional.of(balanceSheet.getUncatIncome()));
  }

  @BeforeEach
  void setUp() {
    Date now = Date.valueOf(LocalDate.now());

    cash = new Balance(balanceSheet, AccountType.debit, "Cash", now, false, "RUB",
        BigDecimal.valueOf(10000L, 2), null);
    when(accountRepo.findById(cash.getId())).thenReturn(Optional.of(cash));

    currentAccount = new Balance(balanceSheet, AccountType.credit, "Current account", now, false, "RUB",
        BigDecimal.valueOf(10000L, 2), null);
    when(accountRepo.findById(currentAccount.getId())).thenReturn(Optional.of(currentAccount));
  }

  @Test
  @Disabled
  void newMoneyOper_simpleExpense() {
    List<Label> labels = new ArrayList<>();
    labels.add(new Label(UUID.randomUUID(), balanceSheet, "label"));
    LocalDate performed = LocalDate.now();
    String comment = "my comment";
    MoneyOper oper = moneyOperService.newMoneyOper(balanceSheet, UUID.randomUUID(), MoneyOperStatus.done, performed, 0, labels,
        comment, Period.month, cash.getId(), balanceSheet.getUncatCosts().getId(), BigDecimal.ONE, BigDecimal.ONE, null, null);

    assertEquals(balanceSheet, oper.getBalanceSheet());

    List<MoneyOperItem> items = oper.getItems();
    assertEquals(1, items.size());

    MoneyOperItem item = items.get(0);
    assertEquals(oper, item.getMoneyOper());
    assertEquals(cash, item.getBalance());
    assertEquals(BigDecimal.ONE.negate(), item.getValue());
    assertEquals(0, item.getIndex());
    assertEquals(performed, item.getPerformed());

    assertEquals(cash.getId(), oper.getFromAccId());
    assertEquals(balanceSheet.getUncatCosts().getId(), oper.getToAccId());

    assertEquals(Period.month, oper.getPeriod());
    assertEquals(MoneyOperStatus.done, oper.getStatus());
    assertEquals(comment, oper.getComment());
    assertEquals(labels, oper.getLabels());
    assertEquals(0, oper.getDateNum().intValue());
  }

  @Test
  @Disabled
  void newMoneyOper_simpleIncome() {
    LocalDate performed = LocalDate.now();
    MoneyOper oper = moneyOperService.newMoneyOper(balanceSheet, UUID.randomUUID(), MoneyOperStatus.done, performed, 0, null,
        "", Period.month, balanceSheet.getUncatIncome().getId(), currentAccount.getId(), BigDecimal.ONE, BigDecimal.ONE, null, null);

    List<MoneyOperItem> items = oper.getItems();
    assertEquals(1, items.size());

    MoneyOperItem item = items.get(0);
    assertEquals(currentAccount, item.getBalance());
    assertEquals(BigDecimal.ONE, item.getValue());
  }

  @Test
  @Disabled
  void newMoneyOper_simpleTransfer() {
    LocalDate performed = LocalDate.now();
    MoneyOper oper = moneyOperService.newMoneyOper(balanceSheet, UUID.randomUUID(), MoneyOperStatus.done, performed, 0, null,
        "", Period.month, currentAccount.getId(), cash.getId(), BigDecimal.ONE, BigDecimal.ONE, null, null);

    List<MoneyOperItem> items = oper.getItems();
    assertEquals(2, items.size());

    MoneyOperItem item0 = items.get(0);
    assertEquals(currentAccount, item0.getBalance());
    assertEquals(BigDecimal.ONE.negate(), item0.getValue());

    MoneyOperItem item1 = items.get(1);
    assertEquals(cash, item1.getBalance());
    assertEquals(BigDecimal.ONE, item1.getValue());
  }

  @Test
  void createMoneyOperByMoneyTrn() {
  }

  @Test
  void moneyOperToMoneyTrn() {
  }

  @Test
  void createReserveMoneyOper() {
  }


  @Test
  void getLabelsByStrings() {
  }

  @Test
  void getStringsByLabels() {
  }


}