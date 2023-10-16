package ru.serdtsev.homemoney.domain.usecase.moneyoper

import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.domain.DomainBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus.*
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatusChanged
import ru.serdtsev.homemoney.domain.model.moneyoper.Period
import ru.serdtsev.homemoney.domain.model.moneyoper.RecurrenceOper
import ru.serdtsev.homemoney.domain.repository.RecurrenceOperRepository
import java.math.BigDecimal
import java.time.LocalDate

internal class CreateMoneyOperUseCaseTest : DomainBaseTest() {
    private val recurrenceOperRepository: RecurrenceOperRepository = mock { }
    private val balanceRepository = repositoryRegistry.balanceRepository
    private val moneyOperRepository = repositoryRegistry.moneyOperRepository
    private val useCase = CreateMoneyOperUseCase(recurrenceOperRepository, moneyOperRepository)

    // todo Перенести сюда кейсы из MoneyOperControllerTest

    @Test
    fun run_done() {
        val balance = Balance(AccountType.debit, "Cash")
        whenever(balanceRepository.findById(balance.id)).thenReturn(balance)
        val sample =  MoneyOper(done, LocalDate.now().minusMonths(1), period = Period.month)
            .apply {this.addItem(balance, BigDecimal("1.00")) }

        whenever(moneyOperRepository.findById(sample.id)).thenReturn(sample)
        whenever(domainEventPublisher.publish(any())).doAnswer { invocation ->
            val model = invocation.arguments[0]
            if (model is MoneyOper && model.status == template) {
                whenever(moneyOperRepository.findById(model.id)).thenReturn(model)
            }
        }

        val recurrenceOper = RecurrenceOper.of(sample)
        val moneyOper = recurrenceOper.createNextMoneyOper().apply { this.status = done }

        whenever(recurrenceOperRepository.findById(recurrenceOper.id)).thenReturn(recurrenceOper)

        val moneyOper0 = MoneyOper(done, dateNum = 0)
        val moneyOper1 = MoneyOper(done, dateNum = 1)
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

        verify(domainEventPublisher, atLeastOnce()).publish(recurrenceOper)

        assertThat(actual)
            .hasSize(1)
            .first()
            .extracting("status", "dateNum")
            .contains(done, 2)
        verify(domainEventPublisher, atLeastOnce()).publish(moneyOper)

        val moneyOperStatusChanged = MoneyOperStatusChanged(new, done, moneyOper)
        verify(domainEventPublisher).publish(moneyOperStatusChanged)
    }
}