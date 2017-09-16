package ru.serdtsev.homemoney.moneyoper;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.serdtsev.homemoney.account.AccountType;
import ru.serdtsev.homemoney.account.Balance;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static ru.serdtsev.homemoney.moneyoper.MoneyOperStatus.pending;

class BalanceChangeTest {
  private BalanceSheet balanceSheet;
  private Balance cash;
  private Balance checkingAccount;
  private MoneyOper oper;

  @BeforeEach
  void setUp() {
    balanceSheet = BalanceSheet.newInstance();
    cash = new Balance(balanceSheet, AccountType.debit, "Cash", Date.valueOf(LocalDate.now()), false,
        "RUB", BigDecimal.TEN, null);
    checkingAccount = new Balance(balanceSheet, AccountType.debit, "Checking account", Date.valueOf(LocalDate.now()), false,
        "RUB", BigDecimal.valueOf(1000L), null);
    oper = new MoneyOper(UUID.randomUUID(), balanceSheet, pending, Date.valueOf(LocalDate.now()), 0,
        new ArrayList<>(), "", null);
  }

  @Test
  void hashCodeAndEquals() {
    BalanceChange origChange = oper.addBalanceChange(cash, BigDecimal.ONE.negate());
    BalanceChange change = SerializationUtils.clone(origChange);
    assertEquals(origChange.hashCode(), change.hashCode());
    assertTrue(change.equals(origChange));

    change.setId(UUID.randomUUID());
    assertNotEquals(origChange.hashCode(), change.hashCode());
    assertFalse(change.equals(origChange));

    change = SerializationUtils.clone(origChange);
    change.setBalance(checkingAccount);
    change.setPerformed(Date.valueOf(LocalDate.now().minusDays(1L)));
    change.setValue(change.getValue().add(BigDecimal.ONE));
    change.setIndex(change.getIndex() + 1);
    assertEquals(origChange.hashCode(), change.hashCode());
    assertTrue(change.equals(origChange));
  }

  @Test
  void essentialEquals() {
    BalanceChange origChange = oper.addBalanceChange(cash, BigDecimal.ONE.negate());
    BalanceChange change = SerializationUtils.clone(origChange);
    assertTrue(change.essentialEquals(origChange));

    change.setPerformed(Date.valueOf(LocalDate.now().minusDays(1L)));
    change.setIndex(change.getIndex() + 1);
    assertTrue(change.essentialEquals(origChange));

    change = SerializationUtils.clone(origChange);
    change.setBalance(checkingAccount);
    assertFalse(change.essentialEquals(origChange));

    change = SerializationUtils.clone(origChange);
    change.setValue(origChange.getValue().add(BigDecimal.ONE));
    assertFalse(change.essentialEquals(origChange));
  }

}