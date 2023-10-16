package ru.serdtsev.homemoney.domain.usecase.moneyoper

import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.domain.DomainBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatusChanged
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import java.math.BigDecimal

internal class DeleteMoneyOperUseCaseTest: DomainBaseTest() {
    private val moneyOperRepository: MoneyOperRepository = mock { }
    private val useCase = DeleteMoneyOperUseCase(moneyOperRepository)

    @Test
    fun run() {
        val balance = Balance(AccountType.debit, "Cash")

        val origMoneyOper = MoneyOper(MoneyOperStatus.Done).apply {
            this.addItem(balance, BigDecimal("1.00"))
        }

        whenever(moneyOperRepository.findById(origMoneyOper.id)).thenReturn(origMoneyOper)

        doAnswer {
            val actual = it.arguments[0] as MoneyOper
            assertThat(actual)
                .extracting("status")
                .isEqualTo(MoneyOperStatus.Cancelled)
        }.whenever(domainEventPublisher).publish(origMoneyOper)

        useCase.run(origMoneyOper.id)

        verify(domainEventPublisher).publish(origMoneyOper)

        val moneyOperStatusChanged = MoneyOperStatusChanged(MoneyOperStatus.Done, MoneyOperStatus.Cancelled, origMoneyOper)
        verify(domainEventPublisher).publish(moneyOperStatusChanged)
    }
}