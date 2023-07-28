package ru.serdtsev.homemoney.domain.usecase.moneyoper

import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.domain.event.BaseDomainEventPublisherTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import java.math.BigDecimal
import java.time.LocalDate

internal class UpdateMoneyOperUseCaseTest: BaseDomainEventPublisherTest() {
    private val moneyOperRepository: MoneyOperRepository = mock { }
    private val useCase = UpdateMoneyOperUseCase(moneyOperRepository)

    @Test
    fun run() {
        val balance1 = Balance(AccountType.debit, "Balance 1", value = BigDecimal("1.00"))
        val balance2 = Balance(AccountType.debit, "Balance 2")
        val tag1 = Tag(balanceSheet, "Tag 1")
        val tag2 = Tag(balanceSheet, "Tag 2")

        val origMoneyOper = MoneyOper(balanceSheet, MoneyOperStatus.done,
            LocalDate.now().minusDays(1), 0, mutableSetOf(tag1), "Comment 1", Period.month
        ).apply {
            this.addItem(balance1, BigDecimal("1.00"))
        }

        val moneyOper = MoneyOper(origMoneyOper.id, balanceSheet, mutableListOf(), MoneyOperStatus.done,
            LocalDate.now(), 1, mutableSetOf(tag2), "Comment 2", Period.single
        ).apply {
            this.addItem(balance2, BigDecimal("1.00"))
        }

        whenever(moneyOperRepository.findById(moneyOper.id)).thenReturn(origMoneyOper)

        doAnswer {
            val actual = it.arguments[0] as MoneyOper
            if (actual.status == MoneyOperStatus.done) {
                assertThat(actual)
                    .extracting("performed", "dateNum", "tags", "comment", "period")
                    .contains(LocalDate.now(), 1, mutableSetOf(tag2), "Comment 2", Period.single)
            }
        }.whenever(domainEventPublisher).publish(origMoneyOper)

        useCase.run(moneyOper)

        verify(domainEventPublisher, times(2)).publish(origMoneyOper)

        val moneyOperStatusChanged1 = MoneyOperStatusChanged(MoneyOperStatus.done, MoneyOperStatus.cancelled, moneyOper)
        verify(domainEventPublisher).publish(moneyOperStatusChanged1)

        val moneyOperStatusChanged2 = MoneyOperStatusChanged(MoneyOperStatus.cancelled, MoneyOperStatus.done, moneyOper)
        verify(domainEventPublisher).publish(moneyOperStatusChanged2)
    }
}