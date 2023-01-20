package ru.serdtsev.homemoney.moneyoper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import ru.serdtsev.homemoney.account.dao.AccountDao;
import ru.serdtsev.homemoney.account.dao.BalanceDao;
import ru.serdtsev.homemoney.account.model.Account;
import ru.serdtsev.homemoney.account.model.AccountType;
import ru.serdtsev.homemoney.account.model.Balance;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetDao;
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet;
import ru.serdtsev.homemoney.moneyoper.dao.MoneyOperDao;
import ru.serdtsev.homemoney.moneyoper.dao.RecurrenceOperDao;
import ru.serdtsev.homemoney.moneyoper.dao.TagDao;
import ru.serdtsev.homemoney.moneyoper.dto.MoneyOperDto;
import ru.serdtsev.homemoney.moneyoper.model.*;
import ru.serdtsev.homemoney.moneyoper.service.MoneyOperService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MoneyOperServiceTest {
  private MoneyOperService service;
  private MoneyOperDao moneyOperDao;
  private BalanceSheet balanceSheet;
  private AccountDao accountDao;
  private ConversionService conversionService;

  @BeforeEach
  void setUp() {
    var balanceSheetDao = mock(BalanceSheetDao.class);
    balanceSheet = new BalanceSheet();
    doReturn(balanceSheet).when(balanceSheetDao).findByIdOrNull(any());

    moneyOperDao = mock(MoneyOperDao.class);
    accountDao = mock(AccountDao.class);
    var balanceDao = mock(BalanceDao.class);
    conversionService = mock(ConversionService.class);
    service = new MoneyOperService(balanceSheetDao, moneyOperDao, mock(RecurrenceOperDao.class),
        accountDao, balanceDao, mock(TagDao.class));
  }

  @Test
  @Disabled
  void getTagsSuggest() {
    Tag car = newTag("car");
    Tag food = newTag("food");
    Tag clothes = newTag("clothes");
    List<Tag> tagsA = asList(car, food, clothes);
    List<Tag> tagsB = asList(food, clothes);
    List<Tag> tagsC = asList(clothes);
    List<MoneyOper> opers = asList(newMoneyOperWithTags(tagsA), newMoneyOperWithTags(tagsB), newMoneyOperWithTags(tagsC));
    when(moneyOperDao.findByBalanceSheetAndStatusAndPerformedGreaterThan(any(), any(), any()))
        .thenReturn(opers);
    Account account = new Account(UUID.randomUUID(), balanceSheet, AccountType.debit, "Some account name", LocalDate.now(), false);
    when(accountDao.findNameById(any())).thenReturn(account.getName());

    MoneyOper moneyOper = newMoneyOperWithTags(new ArrayList<>());
    MoneyOperDto moneyOperDto = conversionService.convert(moneyOper, MoneyOperDto.class);
    Collection<Tag> checkTags = service.getSuggestTags(moneyOper.getItems().get(0).getBalance().getId(),
            MoneyOperType.expense.name(), null, moneyOperDto.getTags());

    List<Tag> expectedTags = asList(clothes, food, car);
    Assertions.assertIterableEquals(expectedTags, checkTags);
  }

  private MoneyOper newMoneyOperWithTags(List<Tag> tags) {
    MoneyOper moneyOper = new MoneyOper(UUID.randomUUID(), balanceSheet, List.of(), MoneyOperStatus.doneNew, LocalDate.now(), 0,
        tags, null, Period.month);
    var created = LocalDate.now();
    Balance balance1 = new Balance(balanceSheet, AccountType.debit, "Some account name", BigDecimal.ZERO);
    Balance balance2 = new Balance(balanceSheet, AccountType.debit, "Some account name", BigDecimal.ZERO);
    moneyOper.addItem(balance1, BigDecimal.valueOf(1).negate(), LocalDate.now(), 0, UUID.randomUUID());
    moneyOper.addItem(balance2, BigDecimal.ONE, LocalDate.now(), 1, UUID.randomUUID());
    return moneyOper;
  }

  private Tag newTag(String name) {
    return new Tag(balanceSheet, name);
  }

}