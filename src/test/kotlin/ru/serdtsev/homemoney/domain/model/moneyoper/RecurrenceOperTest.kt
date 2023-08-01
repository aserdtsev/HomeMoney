package ru.serdtsev.homemoney.domain.model.moneyoper

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
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

    @Test
    fun createNextMoneyOper() {
        val balance = Balance(AccountType.debit, "Cash")
        val template = MoneyOper(MoneyOperStatus.template)
        template.addItem(balance, BigDecimal("1.00"))
        val nextDate = template.performed.plusMonths(1)
        val recurrenceOper = RecurrenceOper(template.id, nextDate)

        val moneyOperRepository: MoneyOperRepository = mock { }
        whenever(moneyOperRepository.findById(template.id)).thenReturn(template)
        whenever(repositoryRegistry.moneyOperRepository).thenReturn(moneyOperRepository)

        val actual = recurrenceOper.createNextMoneyOper()

        val expected = MoneyOper(MoneyOperStatus.recurrence, nextDate)
        expected.addItem(balance, BigDecimal("1.00"))
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("id", "created", "items.id", "items.moneyOperId")
            .isEqualTo(expected)
    }

    @Test
    fun skipNextDate() {
        val template = MoneyOper(MoneyOperStatus.template)
        val nextDate = template.performed.plusMonths(1)
        val recurrenceOper = RecurrenceOper(template.id, nextDate)

        val moneyOperRepository: MoneyOperRepository = mock { }
        whenever(moneyOperRepository.findById(template.id)).thenReturn(template)
        whenever(repositoryRegistry.moneyOperRepository).thenReturn(moneyOperRepository)

        val actual = recurrenceOper.skipNextDate()

        val expected = nextDate.plusMonths(1)
        assertEquals(expected, actual)
        assertEquals(expected, recurrenceOper.nextDate)
        verify(domainEventPublisher).publish(recurrenceOper)
    }

    @Test
    fun calcNextDate() {
        val template = MoneyOper(MoneyOperStatus.template)
        val nextDate = template.performed.plusMonths(1)
        val recurrenceOper = RecurrenceOper(template.id, nextDate)

        val moneyOperRepository: MoneyOperRepository = mock { }
        whenever(moneyOperRepository.findById(template.id)).thenReturn(template)
        whenever(repositoryRegistry.moneyOperRepository).thenReturn(moneyOperRepository)

        val actual = recurrenceOper.calcNextDate(nextDate)

        val expected = nextDate.plusMonths(1)
        assertEquals(expected, actual)
    }

    @Test
    fun arc() {
        val template = MoneyOper(MoneyOperStatus.template)
        val nextDate = template.performed.plusMonths(1)
        val recurrenceOper = RecurrenceOper(template.id, nextDate)

        recurrenceOper.arc()

        assertTrue(recurrenceOper.arc)
        verify(domainEventPublisher).publish(recurrenceOper)
    }
}