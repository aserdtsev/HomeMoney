package ru.serdtsev.homemoney.moneyoper.model

import org.apache.commons.lang3.SerializationUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

internal class MoneyOperTest {
    private val balanceSheet = BalanceSheet()
    private val balance1 = Balance(balanceSheet, AccountType.debit, "Balance 1", BigDecimal("100.00"))
    private val balance2 = Balance(balanceSheet, AccountType.debit, "Balance 2", BigDecimal("200.00"))
    private val cash = Balance(balanceSheet, AccountType.debit, "Cash", BigDecimal("10.00"))
    private val checkingAccount = Balance(balanceSheet, AccountType.debit, "Checking account",  BigDecimal("1000.00"))

    @Test
    fun changeBalancesByExpense() {
        val oper = createExpenseFromCash()
        oper.changeBalances(false)
        Assertions.assertEquals(BigDecimal.valueOf(900L, 2), cash.value)
        oper.changeBalances(true)
        Assertions.assertEquals(BigDecimal.valueOf(1000L, 2), cash.value)
    }

    @Test
    fun changeBalancesByIncome() {
        val oper = createIncomeToCash()
        oper.changeBalances(false)
        Assertions.assertEquals(BigDecimal.valueOf(1100L, 2), cash.value)
        oper.changeBalances(true)
        Assertions.assertEquals(BigDecimal.valueOf(1000L, 2), cash.value)
    }

    @Test
    fun changeBalancesByTransfer() {
        val oper = createTransferFromCheckingAccountToCash(MoneyOperStatus.done)
        oper.changeBalances(false)
        Assertions.assertEquals(BigDecimal.valueOf(99900L, 2), checkingAccount.value)
        Assertions.assertEquals(BigDecimal.valueOf(1100L, 2), cash.value)
        oper.changeBalances(true)
        Assertions.assertEquals(BigDecimal.valueOf(100000L, 2), checkingAccount.value)
        Assertions.assertEquals(BigDecimal.valueOf(1000L, 2), cash.value)
    }

    @Test
    fun complete() {
        val oper = createTransferFromCheckingAccountToCash(MoneyOperStatus.pending)
        oper.complete()
        Assertions.assertEquals(BigDecimal.valueOf(99900L, 2), checkingAccount.value)
        Assertions.assertEquals(BigDecimal.valueOf(1100L, 2), cash.value)
        Assertions.assertEquals(MoneyOperStatus.done, oper.status)
    }

    @Test
    fun cancel() {
        val oper = createTransferFromCheckingAccountToCash(MoneyOperStatus.pending)
        oper.complete()
        oper.cancel()
        Assertions.assertEquals(BigDecimal.valueOf(100000L, 2), checkingAccount.value)
        Assertions.assertEquals(BigDecimal.valueOf(1000L, 2), cash.value)
        Assertions.assertEquals(MoneyOperStatus.cancelled, oper.status)
    }

    @Test
    fun balanceEquals() {
        var origOper = createExpenseFromCash()
        var oper = SerializationUtils.clone(origOper)
        assertTrue(MoneyOper.balanceEquals(oper, origOper))
        val item = oper.items[0]
        item.value = item.value.add(BigDecimal.ONE)
        assertFalse(MoneyOper.balanceEquals(oper, origOper))
        oper = SerializationUtils.clone(origOper)
        oper.addItem(cash, BigDecimal.TEN, LocalDate.now(), 0, UUID.randomUUID())
        assertFalse(MoneyOper.balanceEquals(oper, origOper))
        origOper = createTransferFromCheckingAccountToCash(MoneyOperStatus.done)
        oper = SerializationUtils.clone(origOper)
        oper.items.removeAt(0)
        assertFalse(MoneyOper.balanceEquals(oper, origOper))
    }

    @Test
    fun merge() {
        val origTags = listOf(Tag(balanceSheet, "tag1"), Tag(balanceSheet, "tag2"))
        val origOper = MoneyOper(balanceSheet, MoneyOperStatus.done, tags = origTags, comment = "orig comment",
            period = Period.month)
        origOper.addItem(balance1, BigDecimal("-20.00"))

        val newTags = listOf(Tag(balanceSheet, "tag2"), Tag(balanceSheet, "tag3"))
        val newOper = MoneyOper(origOper.id, balanceSheet, status = MoneyOperStatus.done, tags = newTags,
            comment = "new comment", period = Period.single)
        newOper.addItem(balance1, BigDecimal("-30.00"))

        val actual = MoneyOper.merge(newOper, origOper)

        assertThat(origOper)
            .extracting("comment", "period")
            .contains("new comment", Period.single)
        assertThat(origOper.items)
            .first()
            .extracting("balance", "value")
            .contains(balance1, BigDecimal("-30.00"))
        assertThat(origOper.tags).containsAll(newTags)
        assertThat(balance1).extracting("value").isEqualTo(BigDecimal("90.00"))
        assertThat(actual).containsAll(listOf(origOper, balance1))
    }

    @Test
    fun `merge changed balance`() {
        val origTags = listOf(Tag(balanceSheet, "tag1"), Tag(balanceSheet, "tag2"))
        val origOper = MoneyOper(balanceSheet, MoneyOperStatus.done, tags = origTags, comment = "orig comment",
            period = Period.month)
        origOper.addItem(balance1, BigDecimal("-20.00"))

        val newTags = listOf(Tag(balanceSheet, "tag2"), Tag(balanceSheet, "tag3"))
        val newOper = MoneyOper(origOper.id, balanceSheet, status = MoneyOperStatus.done, tags = newTags,
            comment = "new comment", period = Period.single)
        newOper.addItem(balance2, BigDecimal("-30.00"))

        val actual = MoneyOper.merge(newOper, origOper)

        assertThat(origOper.items)
            .first()
            .extracting("balance", "value")
            .contains(balance2, BigDecimal("-30.00"))
        assertThat(balance1).extracting("value").isEqualTo(BigDecimal("120.00"))
        assertThat(balance2).extracting("value").isEqualTo(BigDecimal("170.00"))
        assertThat(actual).containsAll(listOf(origOper, balance1, balance2))
    }

    @Test
    fun merge_pendingToDone() {
        val origOper = MoneyOper(balanceSheet, MoneyOperStatus.pending)
        origOper.addItem(balance1, BigDecimal("-20.00"))

        val newOper = MoneyOper(origOper.id, balanceSheet, status = MoneyOperStatus.done)
        newOper.addItem(balance2, BigDecimal("-20.00"))

        MoneyOper.merge(newOper, origOper)

        assertThat(origOper).extracting("status").isEqualTo(MoneyOperStatus.done)
    }

    private fun createExpenseFromCash(): MoneyOper {
        val oper = MoneyOper(
            UUID.randomUUID(), balanceSheet, mutableListOf(), MoneyOperStatus.done, LocalDate.now(), 0,
            ArrayList(), "", null
        )
        oper.addItem(cash, BigDecimal.ONE.negate(), LocalDate.now(), 0, UUID.randomUUID())
        return oper
    }

    private fun createIncomeToCash(): MoneyOper {
        val oper = MoneyOper(balanceSheet, MoneyOperStatus.done)
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