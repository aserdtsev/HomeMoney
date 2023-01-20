package ru.serdtsev.homemoney.moneyoper

import org.apache.commons.lang3.SerializationUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

internal class MoneyOperTest {
    private lateinit var balanceSheet: BalanceSheet
    private lateinit var cash: Balance
    private lateinit var checkingAccount: Balance

    @BeforeEach
    fun setUp() {
        balanceSheet = BalanceSheet()
        cash = Balance(balanceSheet, AccountType.debit, "Cash", BigDecimal.TEN)
        checkingAccount = Balance(balanceSheet, AccountType.debit, "Checking account", BigDecimal.valueOf(1000L))
    }

    @Test
    fun changeBalancesByExpense() {
        val oper = createExpenseFromCash(MoneyOperStatus.done)
        oper.changeBalances(false)
        assertEquals(BigDecimal.valueOf(900L, 2), cash.value)
        oper.changeBalances(true)
        assertEquals(BigDecimal.valueOf(1000L, 2), cash.value)
    }

    @Test
    fun changeBalancesByIncome() {
        val oper = createIncomeToCash(MoneyOperStatus.done)
        oper.changeBalances(false)
        assertEquals(BigDecimal.valueOf(1100L, 2), cash.value)
        oper.changeBalances(true)
        assertEquals(BigDecimal.valueOf(1000L, 2), cash.value)
    }

    @Test
    fun changeBalancesByTransfer() {
        val oper = createTransferFromCheckingAccountToCash(MoneyOperStatus.done)
        oper.changeBalances(false)
        assertEquals(BigDecimal.valueOf(99900L, 2), checkingAccount.value)
        assertEquals(BigDecimal.valueOf(1100L, 2), cash.value)
        oper.changeBalances(true)
        assertEquals(BigDecimal.valueOf(100000L, 2), checkingAccount.value)
        assertEquals(BigDecimal.valueOf(1000L, 2), cash.value)
    }

    @Test
    fun complete() {
        val oper = createTransferFromCheckingAccountToCash(MoneyOperStatus.pending)
        oper.complete()
        assertEquals(BigDecimal.valueOf(99900L, 2), checkingAccount.value)
        assertEquals(BigDecimal.valueOf(1100L, 2), cash.value)
        assertEquals(MoneyOperStatus.done, oper.status)
    }

    @Test
    fun cancel() {
        val oper = createTransferFromCheckingAccountToCash(MoneyOperStatus.pending)
        oper.complete()
        oper.cancel()
        assertEquals(BigDecimal.valueOf(100000L, 2), checkingAccount.value)
        assertEquals(BigDecimal.valueOf(1000L, 2), cash.value)
        assertEquals(MoneyOperStatus.cancelled, oper.status)
    }

    @Test
    @Disabled
    fun essentialEquals() {
        var origOper = createExpenseFromCash(MoneyOperStatus.done)
        var oper = SerializationUtils.clone(origOper)
        Assertions.assertTrue(oper.mostlyEquals(origOper))
        val item = oper.items[0]
        item.value = item.value.add(BigDecimal.ONE)
        Assertions.assertFalse(oper.mostlyEquals(origOper))
        oper = SerializationUtils.clone(origOper)
        oper.addItem(cash, BigDecimal.TEN, LocalDate.now(), 0, UUID.randomUUID())
        Assertions.assertFalse(oper.mostlyEquals(origOper))
        origOper = createTransferFromCheckingAccountToCash(MoneyOperStatus.done)
        oper = SerializationUtils.clone(origOper)
        oper.items.removeAt(0)
        Assertions.assertFalse(oper.mostlyEquals(origOper))
    }

    private fun createExpenseFromCash(status: MoneyOperStatus): MoneyOper {
        val oper = MoneyOper(
            UUID.randomUUID(), balanceSheet, mutableListOf(), status, LocalDate.now(), 0,
            ArrayList(), "", null
        )
        oper.addItem(cash, BigDecimal.ONE.negate(), LocalDate.now(), 0, UUID.randomUUID())
        return oper
    }

    private fun createIncomeToCash(status: MoneyOperStatus): MoneyOper {
        val oper = MoneyOper(balanceSheet, status)
        oper.addItem(cash, BigDecimal.ONE, LocalDate.now(), 0, UUID.randomUUID())
        return oper
    }

    private fun createTransferFromCheckingAccountToCash(status: MoneyOperStatus): MoneyOper {
        val oper = MoneyOper(balanceSheet, status)
        val amount = BigDecimal.ONE
        oper.addItem(checkingAccount, amount.negate(), LocalDate.now(), 0, UUID.randomUUID())
        oper.addItem(cash, amount, LocalDate.now(), 1, UUID.randomUUID())
        return oper
    }
}