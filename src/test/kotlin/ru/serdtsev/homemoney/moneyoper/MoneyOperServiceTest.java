package ru.serdtsev.homemoney.moneyoper;

import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.convert.ConversionService;
import ru.serdtsev.homemoney.account.AccountRepo;
import ru.serdtsev.homemoney.account.BalanceRepo;
import ru.serdtsev.homemoney.account.model.Account;
import ru.serdtsev.homemoney.account.model.AccountType;
import ru.serdtsev.homemoney.account.model.Balance;
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.dao.BalanceSheetRepo;
import ru.serdtsev.homemoney.moneyoper.dao.MoneyOperRepo;
import ru.serdtsev.homemoney.moneyoper.dao.RecurrenceOperRepo;
import ru.serdtsev.homemoney.moneyoper.dao.TagRepo;
import ru.serdtsev.homemoney.moneyoper.dto.MoneyOperDto;
import ru.serdtsev.homemoney.moneyoper.model.*;
import ru.serdtsev.homemoney.moneyoper.service.MoneyOperService;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MoneyOperServiceTest {
  private MoneyOperService service;
  private BalanceSheetRepo balanceSheetRepo;
  private MoneyOperRepo moneyOperRepo;
  private BalanceSheet balanceSheet;
  private AccountRepo accountRepo;
  private BalanceRepo balanceRepo;
  private ConversionService conversionService;

  @BeforeEach
  void setUp() {
    balanceSheetRepo = mock(BalanceSheetRepo.class);
    balanceSheet = BalanceSheet.Companion.newInstance();
    doReturn(Optional.of(balanceSheet)).when(balanceSheetRepo).findById(any());

    moneyOperRepo = mock(MoneyOperRepo.class);
    accountRepo = mock(AccountRepo.class);
    val conversionService = mock(ConversionService.class);
    service = new MoneyOperService(balanceSheetRepo, moneyOperRepo, mock(RecurrenceOperRepo.class),
        accountRepo, balanceRepo, mock(TagRepo.class));
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
    when(moneyOperRepo.findByBalanceSheetAndStatusAndPerformedGreaterThan(any(), any(), any()))
        .thenReturn(opers);
    Account account = new Account(UUID.randomUUID(), balanceSheet, AccountType.debit, "Some account name", java.sql.Date.valueOf(LocalDate.now()), false);
    when(accountRepo.findById(any())).thenReturn(Optional.of(account));

    MoneyOper moneyOper = newMoneyOperWithTags(new ArrayList<>());
    MoneyOperDto moneyOperDto = conversionService.convert(moneyOper, MoneyOperDto.class);
    Collection<Tag> checkTags = service.getSuggestTags(moneyOper.getItems().get(0).getBalanceId(),
            MoneyOperType.expense.name(), null, moneyOperDto.getTags());

    List<Tag> expectedTags = asList(clothes, food, car);
    Assertions.assertIterableEquals(expectedTags, checkTags);
  }

  private MoneyOper newMoneyOperWithTags(List<Tag> tags) {
    MoneyOper moneyOper = new MoneyOper(UUID.randomUUID(), balanceSheet, MoneyOperStatus.doneNew, LocalDate.now(), 0,
        tags, null, Period.month);
    Date created = Date.valueOf(LocalDate.now());
    Balance balance1 = new Balance(UUID.randomUUID(), balanceSheet, AccountType.debit, "Some account name", created,
        false, BigDecimal.ZERO, "RUB");
    Balance balance2 = new Balance(UUID.randomUUID(), balanceSheet, AccountType.debit, "Some account name", created,
            false, BigDecimal.ZERO, "RUB");
    moneyOper.addItem(balance1, BigDecimal.valueOf(1).negate(), LocalDate.now(), 0, UUID.randomUUID());
    moneyOper.addItem(balance2, BigDecimal.ONE, LocalDate.now(), 1, UUID.randomUUID());
    return moneyOper;
  }

  private Tag newTag(String name) {
    return new Tag(UUID.randomUUID(), Mockito.mock(BalanceSheet.class), name);
  }

}