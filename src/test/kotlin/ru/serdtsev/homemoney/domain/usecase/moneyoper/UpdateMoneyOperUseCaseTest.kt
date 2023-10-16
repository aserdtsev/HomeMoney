package ru.serdtsev.homemoney.domain.usecase.moneyoper

import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.domain.DomainBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import java.math.BigDecimal
import java.time.LocalDate

internal class UpdateMoneyOperUseCaseTest: DomainBaseTest() {
    private val balanceRepository = repositoryRegistry.balanceRepository
    private val moneyOperRepository = repositoryRegistry.moneyOperRepository
    private val useCase = UpdateMoneyOperUseCase(moneyOperRepository)

    @Test
    fun run() {
        val balance1 = Balance(AccountType.debit, "Balance 1", value = BigDecimal("1.00"))
            .apply { whenever(balanceRepository.findById(id)).thenReturn(this) }
        val balance2 = Balance(AccountType.debit, "Balance 2")
            .apply { whenever(balanceRepository.findById(id)).thenReturn(this) }
        val tag1 = Tag.of("Tag 1")
        val tag2 = Tag.of("Tag 2")

        val origMoneyOper = MoneyOper(MoneyOperStatus.Done, LocalDate.now().minusDays(1),
            mutableSetOf(tag1), "Comment 1", Period.month, dateNum = 0
        ).apply {
            this.addItem(balance1, BigDecimal("1.00"))
        }

        val moneyOper = MoneyOper(origMoneyOper.id, mutableListOf(), MoneyOperStatus.Done,
            LocalDate.now(), mutableSetOf(tag2), "Comment 2", Period.single, dateNum = 1
        ).apply {
            this.addItem(balance2, BigDecimal("1.00"))
        }

        whenever(moneyOperRepository.findById(moneyOper.id)).thenReturn(origMoneyOper)

        doAnswer {
            val actual = it.arguments[0] as MoneyOper
            if (actual.status == MoneyOperStatus.Done) {
                assertThat(actual)
                    .extracting("performed", "dateNum", "tags", "comment", "period")
                    .contains(LocalDate.now(), 1, mutableSetOf(tag2), "Comment 2", Period.single)
            }
        }.whenever(domainEventPublisher).publish(origMoneyOper)

        useCase.run(moneyOper)

        verify(domainEventPublisher, times(2)).publish(origMoneyOper)

        val moneyOperStatusChanged1 = MoneyOperStatusChanged(MoneyOperStatus.Done, MoneyOperStatus.Cancelled, moneyOper)
        verify(domainEventPublisher).publish(moneyOperStatusChanged1)

        val moneyOperStatusChanged2 = MoneyOperStatusChanged(MoneyOperStatus.Cancelled, MoneyOperStatus.Done, moneyOper)
        verify(domainEventPublisher).publish(moneyOperStatusChanged2)
    }
}