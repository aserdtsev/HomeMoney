package ru.serdtsev.homemoney.domain.usecase.moneyoper

import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.domain.DomainBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import ru.serdtsev.homemoney.domain.model.moneyoper.Period
import ru.serdtsev.homemoney.domain.model.moneyoper.RecurrenceOper
import ru.serdtsev.homemoney.domain.repository.RecurrenceOperRepository
import java.math.BigDecimal
import java.time.LocalDate

internal class SkipMoneyOperUseCaseTest: DomainBaseTest() {
    private val recurrenceOperRepository: RecurrenceOperRepository = mock { }
    private val balanceRepository = repositoryRegistry.balanceRepository
    private val moneyOperRepository = repositoryRegistry.moneyOperRepository
    private val useCase = SkipMoneyOperUseCase(recurrenceOperRepository, moneyOperRepository)

    @Test
    fun run_byPendingMoneyOper() {
        val balance = Balance(AccountType.debit, "Cash")
        whenever(balanceRepository.findById(balance.id)).thenReturn(balance)
        val moneyOper = MoneyOper(MoneyOperStatus.Pending).apply {
            this.addItem(balance, BigDecimal("1.00"))
        }

        doAnswer {
            val actual = it.arguments[0] as MoneyOper
            assertThat(actual)
                .extracting("status")
                .isEqualTo(MoneyOperStatus.Cancelled)
        }.whenever(domainEventPublisher).publish(moneyOper)

        useCase.run(moneyOper)

        verify(domainEventPublisher).publish(moneyOper)
    }

    @Test
    fun run_byRecurrenceOper() {
        val balance = Balance(AccountType.debit, "Cash")
            .apply { whenever(balanceRepository.findById(id)).thenReturn(this) }
        val sample =  MoneyOper(MoneyOperStatus.Done, LocalDate.now().minusMonths(1)).apply {
            this.addItem(balance, BigDecimal("1.00"))
            this.period = Period.Month
        }

        whenever(moneyOperRepository.findById(sample.id)).thenReturn(sample)
        whenever(repositoryRegistry.moneyOperRepository).thenReturn(moneyOperRepository)
        whenever(domainEventPublisher.publish(any())).doAnswer { invocation ->
            val model = invocation.arguments[0]
            if (model is MoneyOper && model.status == MoneyOperStatus.Template) {
                whenever(moneyOperRepository.findById(model.id)).thenReturn(model)
            }
        }

        val recurrenceOper = RecurrenceOper.of(sample)
        val moneyOper = MoneyOper(MoneyOperStatus.Recurrence, recurrenceId = recurrenceOper.id).apply {
            this.addItem(balance, BigDecimal("1.00"))
        }

        whenever(recurrenceOperRepository.findById(recurrenceOper.id)).thenReturn(recurrenceOper)

        doAnswer {
            val publishedRecurrenceOper = it.arguments[0] as RecurrenceOper
            assertEquals(LocalDate.now().plusMonths(1), publishedRecurrenceOper.nextDate)
        }.whenever(domainEventPublisher).publish(recurrenceOper)

        useCase.run(moneyOper)

        verify(domainEventPublisher, atLeastOnce()).publish(recurrenceOper)
    }
}