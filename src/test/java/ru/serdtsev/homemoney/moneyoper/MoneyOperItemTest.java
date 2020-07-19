package ru.serdtsev.homemoney.moneyoper;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.serdtsev.homemoney.account.model.AccountType;
import ru.serdtsev.homemoney.account.model.Balance;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperItem;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus.pending;

class MoneyOperItemTest {
  private Balance cash;
  private Balance checkingAccount;
  private MoneyOper oper;

  @BeforeEach
  void setUp() {
    BalanceSheet balanceSheet = BalanceSheet.Companion.newInstance();
    cash = new Balance(balanceSheet, AccountType.debit, "Cash", Date.valueOf(LocalDate.now()), false,
        "RUB", BigDecimal.TEN, null);
    checkingAccount = new Balance(balanceSheet, AccountType.debit, "Checking account", Date.valueOf(LocalDate.now()), false,
        "RUB", BigDecimal.valueOf(1000L), null);
    oper = new MoneyOper(UUID.randomUUID(), balanceSheet, pending, LocalDate.now(), 0,
        new ArrayList<>(), "", null);
  }

  @Test
  @Disabled
  void essentialEquals() {
    MoneyOperItem origItem = oper.addItem(cash, BigDecimal.ONE.negate());
    MoneyOperItem item = SerializationUtils.clone(origItem);
    assertTrue(item.essentialEquals(origItem));

    item.setPerformed(LocalDate.now().minusDays(1L));
    item.setIndex(item.getIndex() + 1);
    assertTrue(item.essentialEquals(origItem));

    item = SerializationUtils.clone(origItem);
    item.setBalance(checkingAccount);
    assertFalse(item.essentialEquals(origItem));

    item = SerializationUtils.clone(origItem);
    item.setValue(origItem.getValue().add(BigDecimal.ONE));
    assertFalse(item.essentialEquals(origItem));
  }

}