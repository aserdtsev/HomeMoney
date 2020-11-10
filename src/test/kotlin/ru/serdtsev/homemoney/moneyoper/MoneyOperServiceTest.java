package ru.serdtsev.homemoney.moneyoper;

import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.convert.ConversionService;
import ru.serdtsev.homemoney.account.AccountRepository;
import ru.serdtsev.homemoney.account.BalanceRepository;
import ru.serdtsev.homemoney.account.model.Account;
import ru.serdtsev.homemoney.account.model.AccountType;
import ru.serdtsev.homemoney.account.model.Balance;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.moneyoper.model.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

class MoneyOperServiceTest {
  private MoneyOperService service;
  private BalanceSheetRepository balanceSheetRepo;
  private MoneyOperRepo moneyOperRepo;
  private BalanceSheet balanceSheet;
  private AccountRepository accountRepo;
  private BalanceRepository balanceRepo;

  @BeforeEach
  void setUp() {
    balanceSheetRepo = mock(BalanceSheetRepository.class);
    balanceSheet = BalanceSheet.Companion.newInstance();
    doReturn(Optional.of(balanceSheet)).when(balanceSheetRepo).findById(any());

    moneyOperRepo = mock(MoneyOperRepo.class);
    accountRepo = mock(AccountRepository.class);
    val conversionService = mock(ConversionService.class);
    service = new MoneyOperService(conversionService, balanceSheetRepo, moneyOperRepo, mock(RecurrenceOperRepo.class),
        accountRepo, balanceRepo, mock(LabelRepository.class));
  }

  @Test
  @Disabled
  void getLabelsSuggest() {
    Label car = newLabel("car");
    Label food = newLabel("food");
    Label clothes = newLabel("clothes");
    List<Label> labelsA = asList(car, food, clothes);
    List<Label> labelsB = asList(food, clothes);
    List<Label> labelsC = asList(clothes);
    List<MoneyOper> opers = asList(newMoneyOperWithLabels(labelsA), newMoneyOperWithLabels(labelsB), newMoneyOperWithLabels(labelsC));
    when(moneyOperRepo.findByBalanceSheetAndStatusAndPerformedGreaterThan(any(), any(), any()))
        .thenReturn(opers);
    Account account = new Account(UUID.randomUUID(), balanceSheet, AccountType.debit, "Some account name", java.sql.Date.valueOf(LocalDate.now()), false);
    when(accountRepo.findById(any())).thenReturn(Optional.of(account));

    MoneyOper moneyOper = newMoneyOperWithLabels(new ArrayList<>());
    MoneyOperDto moneyOperDto = service.moneyOperToDto(moneyOper);
    Collection<Label> checkLabels = service.getSuggestLabels(moneyOper.getItems().get(0).getBalanceId(),
            MoneyOperType.expense.name(), null, moneyOperDto.getLabels());

    List<Label> expectedLabels = asList(clothes, food, car);
    Assertions.assertIterableEquals(expectedLabels, checkLabels);
  }

  private MoneyOper newMoneyOperWithLabels(List<Label> labelsA) {
    MoneyOper moneyOper = new MoneyOper(UUID.randomUUID(), balanceSheet, MoneyOperStatus.doneNew, LocalDate.now(), 0,
        labelsA, null, Period.month);
    Date created = Date.valueOf(LocalDate.now());
    Balance balance1 = new Balance(UUID.randomUUID(), balanceSheet, AccountType.debit, "Some account name", created,
        false, BigDecimal.ZERO, "RUB");
    Balance balance2 = new Balance(UUID.randomUUID(), balanceSheet, AccountType.debit, "Some account name", created,
            false, BigDecimal.ZERO, "RUB");
    moneyOper.addItem(balance1, BigDecimal.valueOf(1).negate(), LocalDate.now(), 0, UUID.randomUUID());
    moneyOper.addItem(balance2, BigDecimal.ONE, LocalDate.now(), 1, UUID.randomUUID());
    return moneyOper;
  }

  private Label newLabel(String name) {
    return new Label(UUID.randomUUID(), Mockito.mock(BalanceSheet.class), name);
  }

}