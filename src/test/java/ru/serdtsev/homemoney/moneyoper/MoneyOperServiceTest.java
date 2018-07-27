package ru.serdtsev.homemoney.moneyoper;

import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import ru.serdtsev.homemoney.account.AccountRepository;
import ru.serdtsev.homemoney.account.model.Account;
import ru.serdtsev.homemoney.account.model.AccountType;
import ru.serdtsev.homemoney.account.model.Balance;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.moneyoper.model.Label;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

class MoneyOperServiceTest {
  private MoneyOperService service;
  private BalanceSheetRepository balanceSheetRepo;
  private MoneyOperRepo moneyOperRepo;
  private BalanceSheet balanceSheet;
  private AccountRepository accountRepo;

  @BeforeEach
  void setUp() {
    balanceSheetRepo = mock(BalanceSheetRepository.class);
    balanceSheet = BalanceSheet.newInstance();
    doReturn(balanceSheet).when(balanceSheetRepo).findOne(any());

    moneyOperRepo = mock(MoneyOperRepo.class);
    accountRepo = mock(AccountRepository.class);
    val conversionService = mock(ConversionService.class);
    service = new MoneyOperService(conversionService, balanceSheetRepo, moneyOperRepo, mock(RecurrenceOperRepo.class),
        accountRepo, mock(LabelRepository.class));
  }

  @Test
  void getLabelsSuggest() {
    Label car = newLabel("car");
    Label food = newLabel("food");
    Label clothes = newLabel("clothes");
    List<Label> labelsA = asList(car, food, clothes);
    List<Label> labelsB = asList(food, clothes);
    List<Label> labelsC = asList(clothes);
    Stream<MoneyOper> opers = Stream.of(newMoneyOperWithLabels(labelsA), newMoneyOperWithLabels(labelsB), newMoneyOperWithLabels(labelsC));
    when(moneyOperRepo.findByBalanceSheetAndStatusAndPerformedGreaterThan(any(), any(), any()))
        .thenReturn(opers);
    Account account = new Account(balanceSheet, AccountType.debit, "Some account name", java.sql.Date.valueOf(LocalDate.now()), false);
    when(accountRepo.findOne(any())).thenReturn(account);

    MoneyOper moneyOper = newMoneyOperWithLabels(new ArrayList<>());
    MoneyOperDto moneyOperDto = service.moneyOperToDto(moneyOper);
    Collection<Label> checkLabels = service.getSuggestLabels(null, moneyOperDto);

    List<Label> expectedLabels = asList(clothes, food, car);
    Assertions.assertIterableEquals(expectedLabels, checkLabels);
  }

  private MoneyOper newMoneyOperWithLabels(List<Label> labelsA) {
    MoneyOper moneyOper = new MoneyOper(UUID.randomUUID(), balanceSheet, null, null, null,
        labelsA, null, null);
    moneyOper.setAmount(BigDecimal.ONE);
    moneyOper.setToAmount(BigDecimal.ONE);
    Balance balance = new Balance(balanceSheet, AccountType.debit, "Some account name", java.sql.Date.valueOf(LocalDate.now()),
        false, "RUB", BigDecimal.ZERO, BigDecimal.ZERO);
    moneyOper.addItem(balance, BigDecimal.ONE);
    return moneyOper;
  }

  private Label newLabel(String name) {
    return new Label(null, null, name);
  }

}