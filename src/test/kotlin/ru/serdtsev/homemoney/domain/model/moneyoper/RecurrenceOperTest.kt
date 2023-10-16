package ru.serdtsev.homemoney.domain.model.moneyoper

import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.domain.DomainBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import java.math.BigDecimal

internal class RecurrenceOperTest : DomainBaseTest() {
    private val moneyOperRepository: MoneyOperRepository = mock { }

    @Test
    fun createNextMoneyOper() {
        val balance = Balance(AccountType.debit, "Cash")
        whenever(repositoryRegistry.balanceRepository.findById(balance.id)).thenReturn(balance)
        val sample = MoneyOper(MoneyOperStatus.Template).apply {
            addItem(balance, BigDecimal("1.00"))
        }
        val nextDate = sample.performed.plusMonths(1)

        whenever(moneyOperRepository.findById(sample.id)).thenReturn(sample)
        whenever(domainEventPublisher.publish(any())).doAnswer { invocation ->
            val model = invocation.arguments[0]
            if (model is MoneyOper && model.status == MoneyOperStatus.Template) {
                whenever(moneyOperRepository.findById(model.id)).thenReturn(model)
            }
        }
        val recurrenceOper = RecurrenceOper.of(sample)

        val actual = recurrenceOper.createNextMoneyOper()

        val expected = MoneyOper(MoneyOperStatus.Recurrence, nextDate, recurrenceId = recurrenceOper.id).apply {
            addItem(balance, BigDecimal("1.00"))
        }
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("id", "created", "items.id", "items.moneyOperId")
            .isEqualTo(expected)
    }

    @Test
    fun skipNextDate() {
        val sample = MoneyOper(MoneyOperStatus.Done)
        val nextDate = sample.performed.plusMonths(1)

        whenever(moneyOperRepository.findById(sample.id)).thenReturn(sample)
        whenever(domainEventPublisher.publish(any())).doAnswer { invocation ->
            val model = invocation.arguments[0]
            if (model is MoneyOper && model.status == MoneyOperStatus.Template) {
                whenever(moneyOperRepository.findById(model.id)).thenReturn(model)
            }
        }
        val recurrenceOper = RecurrenceOper.of(sample)

        val actual = recurrenceOper.skipNextDate()

        val expected = nextDate.plusMonths(1)
        assertEquals(expected, actual)
        assertEquals(expected, recurrenceOper.nextDate)
        verify(domainEventPublisher, atLeastOnce()).publish(recurrenceOper)
    }

    @Test
    fun calcNextDate() {
        val sample = MoneyOper(MoneyOperStatus.Done)
        val nextDate = sample.performed.plusMonths(1)

        whenever(moneyOperRepository.findById(sample.id)).thenReturn(sample)
        whenever(domainEventPublisher.publish(any())).doAnswer { invocation ->
            val model = invocation.arguments[0]
            if (model is MoneyOper && model.status == MoneyOperStatus.Template) {
                whenever(moneyOperRepository.findById(model.id)).thenReturn(model)
            }
        }
        val recurrenceOper = RecurrenceOper.of(sample)

        val actual = recurrenceOper.calcNextDate(nextDate)

        val expected = nextDate.plusMonths(1)
        assertEquals(expected, actual)
    }

    @Test
    fun arc() {
        val sample = MoneyOper(MoneyOperStatus.Done)

        whenever(moneyOperRepository.findById(sample.id)).thenReturn(sample)
        whenever(domainEventPublisher.publish(any())).doAnswer { invocation ->
            val model = invocation.arguments[0]
            if (model is MoneyOper && model.status == MoneyOperStatus.Template) {
                whenever(moneyOperRepository.findById(model.id)).thenReturn(model)
            }
        }
        val recurrenceOper = RecurrenceOper.of(sample)

        recurrenceOper.arc()

        assertTrue(recurrenceOper.arc)
        verify(domainEventPublisher, atLeastOnce()).publish(recurrenceOper)
    }
}