package ru.serdtsev.homemoney.moneyoper;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.serdtsev.homemoney.account.model.AccountType;
import ru.serdtsev.homemoney.account.model.Balance;
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperItem;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus.cancelled;
import static ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus.done;
import static ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus.pending;

class MoneyOperTest {
  private BalanceSheet balanceSheet;
  private Balance cash;
  private Balance checkingAccount;

  @BeforeEach
  void setUp() {
    balanceSheet = BalanceSheet.Companion.newInstance();
    cash = new Balance(UUID.randomUUID(), balanceSheet, AccountType.debit, "Cash", Date.valueOf(LocalDate.now()), false,
        BigDecimal.TEN, "RUB");
    checkingAccount = new Balance(UUID.randomUUID(), balanceSheet, AccountType.debit, "Checking account",
            Date.valueOf(LocalDate.now()), false,BigDecimal.valueOf(1000L), "RUB");
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
  @Disabled
  void essentialEquals() {
    MoneyOper origOper = createExpenseFromCash(done);

    MoneyOper oper = SerializationUtils.clone(origOper);
    assertTrue(oper.essentialEquals(origOper));

    MoneyOperItem item = oper.getItems().get(0);
    item.setValue(item.getValue().add(BigDecimal.ONE));
    assertFalse(oper.essentialEquals(origOper));

    oper = SerializationUtils.clone(origOper);
    oper.addItem(cash, BigDecimal.TEN, LocalDate.now(), 0, UUID.randomUUID());
    assertFalse(oper.essentialEquals(origOper));

    origOper = createTransferFromCheckingAccountToCash(done);
    oper = SerializationUtils.clone(origOper);
    oper.getItems().remove(0);
    assertFalse(oper.essentialEquals(origOper));
  }

  private MoneyOper createExpenseFromCash(MoneyOperStatus status) {
    MoneyOper oper = new MoneyOper(UUID.randomUUID(), balanceSheet, status, LocalDate.now(), 0,
        new ArrayList<>(), "", null);
    oper.addItem(cash, BigDecimal.ONE.negate(), LocalDate.now(), 0, UUID.randomUUID());
    return oper;
  }

  private MoneyOper createIncomeToCash(MoneyOperStatus status) {
    MoneyOper oper = new MoneyOper(UUID.randomUUID(), balanceSheet, status, LocalDate.now(), 0,
        new ArrayList<>(), "", null);
    oper.addItem(cash, BigDecimal.ONE, LocalDate.now(), 0, UUID.randomUUID());
    return oper;
  }

  private MoneyOper createTransferFromCheckingAccountToCash(MoneyOperStatus status) {
    MoneyOper oper = new MoneyOper(UUID.randomUUID(), balanceSheet, status, LocalDate.now(), 0,
        new ArrayList<>(), "", null);
    BigDecimal amount = BigDecimal.ONE;
    oper.addItem(checkingAccount, amount.negate(), LocalDate.now(), 0, UUID.randomUUID());
    oper.addItem(cash, amount, LocalDate.now(), 0, UUID.randomUUID());
    return oper;
  }

}