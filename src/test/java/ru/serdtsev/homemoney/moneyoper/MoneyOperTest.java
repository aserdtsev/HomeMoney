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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.serdtsev.homemoney.moneyoper.MoneyOperStatus.cancelled;
import static ru.serdtsev.homemoney.moneyoper.MoneyOperStatus.done;
import static ru.serdtsev.homemoney.moneyoper.MoneyOperStatus.pending;

class MoneyOperTest {
  private BalanceSheet balanceSheet;
  private Balance cash;
  private Balance checkingAccount;

  @BeforeEach
  void setUp() {
    balanceSheet = BalanceSheet.newInstance();
    cash = new Balance(balanceSheet, AccountType.debit, "Cash", Date.valueOf(LocalDate.now()), false,
        "RUB", BigDecimal.TEN, null);
    checkingAccount = new Balance(balanceSheet, AccountType.debit, "Checking account", Date.valueOf(LocalDate.now()), false,
        "RUB", BigDecimal.valueOf(1000L), null);
  }

  @Test
  void changeBalancesByExpense() {
    MoneyOper oper = createExpenseFromCash(done);

    oper.changeBalances(false);
    assertEquals(BigDecimal.valueOf(9L), cash.getValue());

    oper.changeBalances(true);
    assertEquals(BigDecimal.valueOf(10L), cash.getValue());
  }

  @Test
  void changeBalancesByIncome() {
    MoneyOper oper = createIncomeToCash(done);

    oper.changeBalances(false);
    assertEquals(BigDecimal.valueOf(11L), cash.getValue());

    oper.changeBalances(true);
    assertEquals(BigDecimal.valueOf(10L), cash.getValue());
  }

  @Test
  void changeBalancesByTransfer() {
    MoneyOper oper = createTransferFromCheckingAccountToCash(done);

    oper.changeBalances(false);
    assertEquals(BigDecimal.valueOf(999L), checkingAccount.getValue());
    assertEquals(BigDecimal.valueOf(11L), cash.getValue());

    oper.changeBalances(true);
    assertEquals(BigDecimal.valueOf(1000L), checkingAccount.getValue());
    assertEquals(BigDecimal.valueOf(10L), cash.getValue());
  }

  @Test
  void complete() {
    MoneyOper oper = createTransferFromCheckingAccountToCash(pending);
    oper.complete();
    assertEquals(BigDecimal.valueOf(999L), checkingAccount.getValue());
    assertEquals(BigDecimal.valueOf(11L), cash.getValue());
    assertEquals(done, oper.getStatus());
  }

  @Test
  void cancel() {
    MoneyOper oper = createTransferFromCheckingAccountToCash(pending);
    oper.complete();
    oper.cancel();
    assertEquals(BigDecimal.valueOf(1000L), checkingAccount.getValue());
    assertEquals(BigDecimal.valueOf(10L), cash.getValue());
    assertEquals(cancelled, oper.getStatus());
  }

  @Test
  void essentialEquals() {
    MoneyOper origOper = createExpenseFromCash(done);

    MoneyOper oper = SerializationUtils.clone(origOper);
    assertTrue(oper.essentialEquals(origOper));

    MoneyOperItem item = oper.getItems().get(0);
    item.setValue(item.getValue().add(BigDecimal.ONE));
    assertFalse(oper.essentialEquals(origOper));

    oper = SerializationUtils.clone(origOper);
    oper.addItem(cash, BigDecimal.TEN);
    assertFalse(oper.essentialEquals(origOper));

    origOper = createTransferFromCheckingAccountToCash(done);
    oper = SerializationUtils.clone(origOper);
    oper.getItems().remove(0);
    assertFalse(oper.essentialEquals(origOper));
  }

  private MoneyOper createExpenseFromCash(MoneyOperStatus status) {
    MoneyOper oper = new MoneyOper(UUID.randomUUID(), balanceSheet, status, Date.valueOf(LocalDate.now()), 0,
        new ArrayList<>(), "", null);
    oper.addItem(cash, BigDecimal.ONE.negate());
    return oper;
  }

  private MoneyOper createIncomeToCash(MoneyOperStatus status) {
    MoneyOper oper = new MoneyOper(UUID.randomUUID(), balanceSheet, status, Date.valueOf(LocalDate.now()), 0,
        new ArrayList<>(), "", null);
    oper.addItem(cash, BigDecimal.ONE);
    return oper;
  }

  private MoneyOper createTransferFromCheckingAccountToCash(MoneyOperStatus status) {
    MoneyOper oper = new MoneyOper(UUID.randomUUID(), balanceSheet, status, Date.valueOf(LocalDate.now()), 0,
        new ArrayList<>(), "", null);
    BigDecimal amount = BigDecimal.ONE;
    oper.addItem(checkingAccount, amount.negate());
    oper.addItem(cash, amount);
    return oper;
  }

}