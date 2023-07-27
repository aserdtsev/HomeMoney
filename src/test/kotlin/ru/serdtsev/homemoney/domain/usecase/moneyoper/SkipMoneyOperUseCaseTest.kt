package ru.serdtsev.homemoney.domain.usecase.moneyoper

import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.domain.event.BaseDomainEventPublisherTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import ru.serdtsev.homemoney.domain.model.moneyoper.Period
import ru.serdtsev.homemoney.domain.model.moneyoper.RecurrenceOper
import ru.serdtsev.homemoney.domain.repository.RecurrenceOperRepository
import java.math.BigDecimal
import java.time.LocalDate

internal class SkipMoneyOperUseCaseTest: BaseDomainEventPublisherTest() {
    private val recurrenceOperRepository: RecurrenceOperRepository = mock { }
    private val useCase = SkipMoneyOperUseCase(recurrenceOperRepository)

    @Test
    fun run_byPendingMoneyOper() {
        val balance = Balance(balanceSheet, AccountType.debit, "Cash")
        val moneyOper = MoneyOper(balanceSheet, MoneyOperStatus.pending).apply {
            this.addItem(balance, BigDecimal("1.00"))
        }

        doAnswer {
            val actual = it.arguments[0] as MoneyOper
            assertThat(actual)
                .extracting("status")
                .isEqualTo(MoneyOperStatus.cancelled)
        }.whenever(domainEventPublisher).publish(moneyOper)

        useCase.run(moneyOper)

        verify(domainEventPublisher).publish(moneyOper)
    }

    @Test
    fun run_byRecurrenceOper() {
        val balance = Balance(balanceSheet, AccountType.debit, "Cash")
        val template =  MoneyOper(balanceSheet, MoneyOperStatus.done).apply {
            this.addItem(balance, BigDecimal("1.00"))
            this.period = Period.month
        }
        val recurrenceOper = RecurrenceOper(balanceSheet, template, LocalDate.now())
        val moneyOper = MoneyOper(balanceSheet, MoneyOperStatus.recurrence).apply {
            this.recurrenceId = recurrenceOper.id
            this.addItem(balance, BigDecimal("1.00"))
        }

        whenever(recurrenceOperRepository.findById(recurrenceOper.id)).thenReturn(recurrenceOper)

        doAnswer {
            val publishedRecurrenceOper = it.arguments[0] as RecurrenceOper
            assertEquals(LocalDate.now().plusMonths(1), publishedRecurrenceOper.nextDate)
        }.whenever(domainEventPublisher).publish(recurrenceOper)

        useCase.run(moneyOper)

        verify(domainEventPublisher).publish(recurrenceOper)
    }
}