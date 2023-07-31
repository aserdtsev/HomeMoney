package ru.serdtsev.homemoney.domain.model.moneyoper

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import org.apache.commons.lang3.SerializationUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.domain.DomainBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

internal class MoneyOperTest: DomainBaseTest() {
    private val balance1 = Balance(AccountType.debit, "Balance 1", BigDecimal("100.00"))
    private val balance2 = Balance(AccountType.debit, "Balance 2", BigDecimal("200.00"))
    private val cash = Balance(AccountType.debit, "Cash", BigDecimal("10.00"))
    private val checkingAccount = Balance(AccountType.debit, "Checking account",  BigDecimal("1000.00"))

    @Test
    fun complete() {
        val oper = createTransferFromCheckingAccountToCash(pending)

        oper.complete()

        assertEquals(done, oper.status)
        verify(domainEventPublisher).publish(oper)
        checkPublishMoneyOperStatusChanged(pending, done, oper)
    }

    @Test
    internal fun postpone() {
        val oper = createIncomeToCash().apply {
            this.performed = this.performed.plusDays(1)
        }

        oper.postpone()

        assertEquals(pending, oper.status)
        verify(domainEventPublisher).publish(oper)
        checkPublishMoneyOperStatusChanged(done, pending, oper)
    }

    @Test
    fun cancel() {
        val oper = createTransferFromCheckingAccountToCash(done)

        oper.cancel()

        assertEquals(cancelled, oper.status)
        verify(domainEventPublisher).publish(oper)
        checkPublishMoneyOperStatusChanged(done, cancelled, oper)
    }

    @Test
    internal fun skipPending() {
        val oper = createIncomeToCash().apply {
            this.status = pending
        }

        oper.skipPending()

        assertEquals(cancelled, oper.status)
        verify(domainEventPublisher).publish(oper)
        checkPublishMoneyOperStatusChanged(pending, cancelled, oper)
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
        origOper = createTransferFromCheckingAccountToCash(done)
        oper = SerializationUtils.clone(origOper)
        oper.items.removeAt(0)
        assertFalse(MoneyOper.balanceEquals(oper, origOper))
    }

    @Test
    fun merge() {
        val origTags = listOf(Tag("tag1"), Tag("tag2"))
        val origOper = MoneyOper(done, tags = origTags, comment = "orig comment", period = Period.month)
        origOper.addItem(balance1, BigDecimal("-20.00"))

        val newTags = listOf(Tag("tag2"), Tag("tag3"))
        val newOper = MoneyOper(origOper.id, status = done, tags = newTags,
            comment = "new comment", period = Period.single)
        newOper.addItem(balance1, BigDecimal("-30.00"))

        MoneyOper.merge(newOper, origOper)

        assertThat(origOper)
            .extracting("comment", "period")
            .contains("new comment", Period.single)
        assertThat(origOper.items)
            .first()
            .extracting("balance", "value")
            .contains(balance1, BigDecimal("-30.00"))
        assertThat(origOper.tags).containsAll(newTags)

        verify(domainEventPublisher, times(2)).publish(origOper)
        checkPublishMoneyOperStatusChanged(done, cancelled, origOper)
        checkPublishMoneyOperStatusChanged(cancelled, done, origOper)
    }

    @Test
    fun `merge changed balance`() {
        val origTags = listOf(Tag("tag1"), Tag("tag2"))
        val origOper = MoneyOper(done, tags = origTags, comment = "orig comment", period = Period.month)
        origOper.addItem(balance1, BigDecimal("-20.00"))

        val newTags = listOf(Tag("tag2"), Tag("tag3"))
        val newOper = MoneyOper(origOper.id, status = done, tags = newTags,
            comment = "new comment", period = Period.single)
        newOper.addItem(balance2, BigDecimal("-30.00"))

        MoneyOper.merge(newOper, origOper)

        assertThat(origOper.items)
            .first()
            .extracting("balance", "value")
            .contains(balance2, BigDecimal("-30.00"))

        verify(domainEventPublisher, times(2)).publish(origOper)

        checkPublishMoneyOperStatusChanged(done, cancelled, origOper)
        checkPublishMoneyOperStatusChanged(cancelled, done, origOper)
    }

    @Test
    fun merge_pendingToDone() {
        val origOper = MoneyOper(pending)
        origOper.addItem(balance1, BigDecimal("-20.00"))

        val newOper = MoneyOper(origOper.id, status = done)
        newOper.addItem(balance2, BigDecimal("-20.00"))

        MoneyOper.merge(newOper, origOper)

        assertThat(origOper).extracting("status").isEqualTo(done)
    }

    private fun createExpenseFromCash(): MoneyOper {
        val oper = MoneyOper(UUID.randomUUID(), mutableListOf(), done, LocalDate.now(), 0,
            ArrayList(), "", null)
        oper.addItem(cash, BigDecimal.ONE.negate(), LocalDate.now(), 0, UUID.randomUUID())
        return oper
    }

    private fun createIncomeToCash(): MoneyOper {
        val oper = MoneyOper(done)
        oper.addItem(cash, BigDecimal.ONE, LocalDate.now(), 0, UUID.randomUUID())
        return oper
    }

    private fun createTransferFromCheckingAccountToCash(status: MoneyOperStatus): MoneyOper {
        val oper = MoneyOper(status)
        val amount = BigDecimal.ONE
        oper.addItem(checkingAccount, amount.negate(), LocalDate.now(), 0, UUID.randomUUID())
        oper.addItem(cash, amount, LocalDate.now(), 1, UUID.randomUUID())
        return oper
    }

    private fun checkPublishMoneyOperStatusChanged(beforeStatus: MoneyOperStatus, afterStatus: MoneyOperStatus,
                                                   moneyOper: MoneyOper
    ) {
        val moneyOperStatusChanged = MoneyOperStatusChanged(beforeStatus, afterStatus, moneyOper)
        verify(domainEventPublisher).publish(moneyOperStatusChanged)
    }
}