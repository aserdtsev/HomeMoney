package ru.serdtsev.homemoney.domain.usecase.moneyoper

import com.nhaarman.mockito_kotlin.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.domain.DomainBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import java.math.BigDecimal
import java.time.LocalDate

internal class UpMoneyOperUseCaseTest: DomainBaseTest() {
    private val moneyOperRepository: MoneyOperRepository = mock { }
    private val useCase = UpMoneyOperUseCase(moneyOperRepository)

    @Test
    fun `run MoneyOper by last index`() {
        val balance = Balance(AccountType.debit, "Card")
        val moneyOpers = IntRange(0, 2)
            .map { i -> createMoneyOper(balance, i) }
            .toList()
        val moneyOper = moneyOpers[2]

        whenever(moneyOperRepository.findById(moneyOper.id)).thenReturn(moneyOper)
        whenever(moneyOperRepository.findByBalanceSheetAndStatusAndPerformed(balanceSheet.id, MoneyOperStatus.done, LocalDate.now()))
            .thenReturn(moneyOpers)

        useCase.run(moneyOper.id)

        verify(domainEventPublisher, times(0)).publish(moneyOper)
    }

    @Test
    fun `run MoneyOper by first index`() {
        val balance = Balance(AccountType.debit, "Card")
        val moneyOpers = IntRange(0, 2)
            .map { i -> createMoneyOper(balance, i) }
            .toList()
        val moneyOper = moneyOpers[0]
        val nextMoneyOper = moneyOpers[1]

        whenever(moneyOperRepository.findById(moneyOper.id)).thenReturn(moneyOper)
        whenever(moneyOperRepository.findByBalanceSheetAndStatusAndPerformed(balanceSheet.id, MoneyOperStatus.done, LocalDate.now()))
            .thenReturn(moneyOpers)

        doAnswer {
            val aMoneyOper = it.arguments[0] as MoneyOper
            assertEquals(1, aMoneyOper.dateNum)
        }.whenever(domainEventPublisher).publish(moneyOper)

        doAnswer {
            val aMoneyOper = it.arguments[0] as MoneyOper
            assertEquals(0, aMoneyOper.dateNum)
        }.whenever(domainEventPublisher).publish(nextMoneyOper)

        useCase.run(moneyOper.id)

        verify(domainEventPublisher).publish(moneyOper)
        verify(domainEventPublisher).publish(nextMoneyOper)
    }

    private fun createMoneyOper(balance: Balance, index: Int) =
        MoneyOper(MoneyOperStatus.done, dateNum = index).apply {
            this.addItem(balance, BigDecimal("1.00"))
        }
}