package ru.serdtsev.homemoney.domain.usecase.moneyoper

import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.domain.event.BaseDomainEventPublisherTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus.done
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.domain.repository.RecurrenceOperRepository
import java.math.BigDecimal
import java.time.LocalDate

internal class CreateMoneyOperUseCaseTest : BaseDomainEventPublisherTest() {
    private val recurrenceOperRepository: RecurrenceOperRepository = mock { }
    private val moneyOperRepository: MoneyOperRepository = mock { }
    private val useCase = CreateMoneyOperUseCase(recurrenceOperRepository, moneyOperRepository)

    // todo Перенести сюда кейсы из MoneyOperControllerTest

    @Test
    fun run_done() {
        val balance = Balance(AccountType.debit, "Cash")
        val template =  MoneyOper(balanceSheet, done).apply {
            this.addItem(balance, BigDecimal("1.00"))
            this.period = Period.month
        }
        val recurrenceOper = RecurrenceOper(balanceSheet, template, LocalDate.now())
        val moneyOper = MoneyOper(balanceSheet, MoneyOperStatus.doneNew).apply {
            this.recurrenceId = recurrenceOper.id
            this.addItem(balance, BigDecimal("1.00"))
        }

        whenever(recurrenceOperRepository.findById(recurrenceOper.id)).thenReturn(recurrenceOper)

        val moneyOper0 = MoneyOper(balanceSheet, done, dateNum = 0)
        val moneyOper1 = MoneyOper(balanceSheet, done, dateNum = 1)
        whenever(moneyOperRepository.findByBalanceSheetAndStatusAndPerformed(balanceSheet.id, done, moneyOper.performed))
            .thenReturn(listOf(moneyOper, moneyOper0, moneyOper1))

        doAnswer {
            val publishedRecurrenceOper = it.arguments[0] as RecurrenceOper
            assertEquals(LocalDate.now().plusMonths(1), publishedRecurrenceOper.nextDate)
        }.whenever(domainEventPublisher).publish(recurrenceOper)

        doAnswer {
            val publishedMoneyOper = it.arguments[0] as MoneyOper
            assertThat(publishedMoneyOper)
                .extracting("status")
                .isEqualTo(done)
        }.whenever(domainEventPublisher).publish(moneyOper)

        val actual = useCase.run(moneyOper)

        verify(domainEventPublisher).publish(recurrenceOper)

        assertThat(actual)
            .hasSize(1)
            .first()
            .extracting("status", "dateNum")
            .contains(done, 2)
        verify(domainEventPublisher, atLeast(1)).publish(moneyOper)

        val moneyOperStatusChanged = MoneyOperStatusChanged(MoneyOperStatus.doneNew, done, moneyOper)
        verify(domainEventPublisher).publish(moneyOperStatusChanged)
    }
}