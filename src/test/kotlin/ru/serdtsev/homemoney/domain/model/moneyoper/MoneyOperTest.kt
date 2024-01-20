package ru.serdtsev.homemoney.domain.model.moneyoper

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.apache.commons.lang3.SerializationUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.domain.DomainBaseTest
import ru.serdtsev.homemoney.domain.event.MoneyOperStatusChanged
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

    @BeforeEach
    override fun setUp() {
        super.setUp()
        whenever(repositoryRegistry.balanceRepository.findById(balance1.id)).thenReturn(balance1)
        whenever(repositoryRegistry.balanceRepository.findById(balance2.id)).thenReturn(balance2)
        whenever(repositoryRegistry.balanceRepository.findById(cash.id)).thenReturn(cash)
        whenever(repositoryRegistry.balanceRepository.findById(checkingAccount.id)).thenReturn(checkingAccount)
    }

    @Test
    fun complete() {
        val oper = createTransferFromCheckingAccountToCash(Pending)

        oper.complete()

        assertEquals(Done, oper.status)
        verify(domainEventPublisher).publish(oper)
        checkPublishMoneyOperStatusChanged(Pending, Done, oper)
    }

    @Test
    internal fun postpone() {
        val oper = createIncomeToCash().apply {
            this.performed = this.performed.plusDays(1)
        }

        oper.postpone()

        assertEquals(Pending, oper.status)
        verify(domainEventPublisher).publish(oper)
        checkPublishMoneyOperStatusChanged(Done, Pending, oper)
    }

    @Test
    fun cancel() {
        val oper = createTransferFromCheckingAccountToCash(Done)

        oper.cancel()

        assertEquals(Cancelled, oper.status)
        verify(domainEventPublisher).publish(oper)
        checkPublishMoneyOperStatusChanged(Done, Cancelled, oper)
    }

    @Test
    internal fun skipPending() {
        val oper = createIncomeToCash().apply {
            this.status = Pending
        }

        oper.skipPending()

        assertEquals(Cancelled, oper.status)
        verify(domainEventPublisher).publish(oper)
        checkPublishMoneyOperStatusChanged(Pending, Cancelled, oper)
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
        oper.addItem(cash, BigDecimal.TEN, LocalDate.now(), 0, id = UUID.randomUUID())
        assertFalse(MoneyOper.balanceEquals(oper, origOper))

        origOper = createTransferFromCheckingAccountToCash(Done)
        oper = SerializationUtils.clone(origOper)
        oper.items.removeAt(0)
        assertFalse(MoneyOper.balanceEquals(oper, origOper))
    }

    @Test
    fun merge() {
        val origTags = listOf(Tag.of("tag1"), Tag.of("tag2"))
        val origOper = MoneyOper(Done, tags = origTags, comment = "orig comment", period = Period.Month)
        origOper.addItem(balance1, BigDecimal("-20.00"))

        val newTags = listOf(Tag.of("tag2"), Tag.of("tag3"))
        val newOper = MoneyOper(origOper.id, status = Done, tags = newTags,
            comment = "new comment", period = Period.Single)
        newOper.addItem(balance1, BigDecimal("-30.00"))

        MoneyOper.merge(newOper, origOper)

        assertThat(origOper)
            .extracting("comment", "period")
            .contains("new comment", Period.Single)
        assertThat(origOper.items)
            .first()
            .extracting("balance", "value")
            .contains(balance1, BigDecimal("-30.00"))
        assertThat(origOper.tags).containsAll(newTags)

        verify(domainEventPublisher, times(2)).publish(origOper)
        checkPublishMoneyOperStatusChanged(Done, Cancelled, origOper)
        checkPublishMoneyOperStatusChanged(Cancelled, Done, origOper)
    }

    @Test
    fun `merge changed balance`() {
        val origTags = listOf(Tag.of("tag1"), Tag.of("tag2"))
        val origOper = MoneyOper(Done, tags = origTags, comment = "orig comment", period = Period.Month)
        origOper.addItem(balance1, BigDecimal("-20.00"))

        val newTags = listOf(Tag.of("tag2"), Tag.of("tag3"))
        val newOper = MoneyOper(origOper.id, status = Done, tags = newTags,
            comment = "new comment", period = Period.Single)
        newOper.addItem(balance2, BigDecimal("-30.00"))

        MoneyOper.merge(newOper, origOper)

        assertThat(origOper.items)
            .first()
            .extracting("balance", "value")
            .contains(balance2, BigDecimal("-30.00"))

        verify(domainEventPublisher, times(2)).publish(origOper)

        checkPublishMoneyOperStatusChanged(Done, Cancelled, origOper)
        checkPublishMoneyOperStatusChanged(Cancelled, Done, origOper)
    }

    @Test
    fun merge_pendingToDone() {
        val origOper = MoneyOper(Pending)
        origOper.addItem(balance1, BigDecimal("-20.00"))

        val newOper = MoneyOper(origOper.id, status = Done)
        newOper.addItem(balance2, BigDecimal("-20.00"))

        MoneyOper.merge(newOper, origOper)

        assertThat(origOper).extracting("status").isEqualTo(Done)
    }

    private fun createExpenseFromCash(): MoneyOper {
        val oper = MoneyOper(UUID.randomUUID(), mutableListOf(), Done, LocalDate.now(), ArrayList(),
            "", null, dateNum = 0)
        oper.addItem(cash, BigDecimal.ONE.negate(), LocalDate.now(), 0, id = UUID.randomUUID())
        return oper
    }

    private fun createIncomeToCash(): MoneyOper {
        val oper = MoneyOper(Done)
        oper.addItem(cash, BigDecimal.ONE, LocalDate.now(), 0, id = UUID.randomUUID())
        return oper
    }

    private fun createTransferFromCheckingAccountToCash(status: MoneyOperStatus): MoneyOper {
        val oper = MoneyOper(status)
        val amount = BigDecimal.ONE
        oper.addItem(checkingAccount, amount.negate(), LocalDate.now(), 0, id = UUID.randomUUID())
        oper.addItem(cash, amount, LocalDate.now(), 1, id = UUID.randomUUID())
        return oper
    }

    private fun checkPublishMoneyOperStatusChanged(beforeStatus: MoneyOperStatus, afterStatus: MoneyOperStatus,
                                                   moneyOper: MoneyOper
    ) {
        val moneyOperStatusChanged = MoneyOperStatusChanged(beforeStatus, afterStatus, moneyOper)
        verify(domainEventPublisher).publish(moneyOperStatusChanged)
    }
}