package ru.serdtsev.homemoney.moneyoper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.serdtsev.homemoney.account.*;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MoneyTrnsResourceTest {
  private DataSource dataSource = mock(DataSource.class);
  private MoneyTrnsResource mtRes;
  private AccountRepository accountRepo = mock(AccountRepository.class);
  private MoneyOperService moneyOperService = mock(MoneyOperService.class);
  private BalanceSheetRepository balanceSheetRepo = mock(BalanceSheetRepository.class);
  private BalanceRepository balanceRepo = mock(BalanceRepository.class);
  private MoneyOperRepository moneyOperRepo = mock(MoneyOperRepository.class);
  private LabelRepository labelRepo = mock(LabelRepository.class);
  private BalanceChangeRepository balanceChangeRepo = mock(BalanceChangeRepository.class);
  private CategoryRepository categoryRepo = mock(CategoryRepository.class);
  private BalanceSheet balanceSheet = BalanceSheet.newInstance();
  private Balance cash;
  private Balance currentAccount;

  public MoneyTrnsResourceTest() {
    this.mtRes = new MoneyTrnsResource(moneyOperService, balanceSheetRepo, accountRepo, moneyOperRepo, labelRepo,
        balanceChangeRepo, categoryRepo);
    when(accountRepo.findOne(balanceSheet.getUncatCosts().getId())).thenReturn(balanceSheet.getUncatCosts());
    when(accountRepo.findOne(balanceSheet.getUncatIncome().getId())).thenReturn(balanceSheet.getUncatIncome());
  }

  @BeforeEach
  void setUp() {
    Date now = Date.valueOf(LocalDate.now());

    cash = new Balance(balanceSheet, AccountType.debit, "Cash", now, false, "RUB",
        BigDecimal.valueOf(10000L, 2), null);
    when(accountRepo.findOne(cash.getId())).thenReturn(cash);

    currentAccount = new Balance(balanceSheet, AccountType.credit, "Current account", now, false, "RUB",
        BigDecimal.valueOf(10000L, 2), null);
    when(accountRepo.findOne(currentAccount.getId())).thenReturn(currentAccount);
  }

  @Test
  void newMoneyOper_simpleExpense() {
    List<Label> labels = new ArrayList<>();
    labels.add(new Label(UUID.randomUUID(), balanceSheet, "label", null, false));
    java.sql.Date performed = java.sql.Date.valueOf(LocalDate.now());
    String comment = "my comment";
    MoneyOper oper = mtRes.moneyOperService.newMoneyOper(balanceSheet, UUID.randomUUID(), MoneyOperStatus.done, performed, 0, labels,
        comment, Period.month, cash.getId(), balanceSheet.getUncatCosts().getId(), BigDecimal.ONE, BigDecimal.ONE, null, null);

    assertEquals(balanceSheet, oper.getBalanceSheet());

    List<BalanceChange> balanceChanges = oper.getBalanceChanges();
    assertEquals(1, balanceChanges.size());

    BalanceChange balanceChange = balanceChanges.get(0);
    assertEquals(oper, balanceChange.getMoneyOper());
    assertEquals(cash, balanceChange.getBalance());
    assertEquals(BigDecimal.ONE.negate(), balanceChange.getValue());
    assertEquals(0, balanceChange.getIndex());
    assertEquals(performed, balanceChange.getPerformed());

    assertEquals(cash.getId(), oper.getFromAccId());
    assertEquals(balanceSheet.getUncatCosts().getId(), oper.getToAccId());

    assertEquals(Period.month, oper.getPeriod());
    assertEquals(MoneyOperStatus.done, oper.getStatus());
    assertEquals(comment, oper.getComment());
    assertEquals(labels, oper.getLabels());
    assertEquals(0, oper.getDateNum().intValue());
  }

  @Test
  void newMoneyOper_simpleIncome() {
    java.sql.Date performed = java.sql.Date.valueOf(LocalDate.now());
    MoneyOper oper = mtRes.moneyOperService.newMoneyOper(balanceSheet, UUID.randomUUID(), MoneyOperStatus.done, performed, 0, null,
        "", Period.month, balanceSheet.getUncatIncome().getId(), currentAccount.getId(), BigDecimal.ONE, BigDecimal.ONE, null, null);

    List<BalanceChange> balanceChanges = oper.getBalanceChanges();
    assertEquals(1, balanceChanges.size());

    BalanceChange balanceChange = balanceChanges.get(0);
    assertEquals(currentAccount, balanceChange.getBalance());
    assertEquals(BigDecimal.ONE, balanceChange.getValue());
  }

  @Test
  void newMoneyOper_simpleTransfer() {
    java.sql.Date performed = java.sql.Date.valueOf(LocalDate.now());
    MoneyOper oper = mtRes.moneyOperService.newMoneyOper(balanceSheet, UUID.randomUUID(), MoneyOperStatus.done, performed, 0, null,
        "", Period.month, currentAccount.getId(), cash.getId(), BigDecimal.ONE, BigDecimal.ONE, null, null);

    List<BalanceChange> balanceChanges = oper.getBalanceChanges();
    assertEquals(2, balanceChanges.size());

    BalanceChange balanceChange0 = balanceChanges.get(0);
    assertEquals(currentAccount, balanceChange0.getBalance());
    assertEquals(BigDecimal.ONE.negate(), balanceChange0.getValue());

    BalanceChange balanceChange1 = balanceChanges.get(1);
    assertEquals(cash, balanceChange1.getBalance());
    assertEquals(BigDecimal.ONE, balanceChange1.getValue());
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