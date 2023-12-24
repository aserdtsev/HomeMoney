package ru.serdtsev.homemoney.domain.event

import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.*
import ru.serdtsev.homemoney.domain.DomainBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus.*
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatusChanged
import java.math.BigDecimal
import java.util.stream.Stream

internal class ChangeBalanceValueHandlerTest: DomainBaseTest() {
    private val changeBalanceValueHandler = ChangeBalanceValueHandler()

    @ParameterizedTest
    @MethodSource("paramsFor_handler_balanceIsChanged")
    fun handler_balanceIsChanged(beforeStatus: MoneyOperStatus, afterStatus: MoneyOperStatus, expected: BigDecimal) {
        val balance = Balance(AccountType.debit, "Cash", value = BigDecimal("100.00"))
        val moneyOper = MoneyOper(afterStatus)
        moneyOper.addItem(balance, BigDecimal("1.00"))
        val event = MoneyOperStatusChanged(beforeStatus, afterStatus, moneyOper)

        whenever(repositoryRegistry.balanceRepository.findById(balance.id)).thenReturn(balance)
        doAnswer {
            val aBalance= it.arguments[0] as Balance
            assertEquals(expected, aBalance.value)
        }.whenever(domainEventPublisher).publish(balance)

        changeBalanceValueHandler.handler(event)

        verify(domainEventPublisher).publish(balance)
    }

    @ParameterizedTest
    @MethodSource("paramsFor_handler_balanceNotChanged")
    fun handler_balanceNotChanged(beforeStatus: MoneyOperStatus, afterStatus: MoneyOperStatus) {
        val moneyOper = MoneyOper(afterStatus)
        val balance = Balance(AccountType.debit, "Cash", value = BigDecimal("100.00"))
        moneyOper.addItem(balance, BigDecimal("1.00"))
        val event = MoneyOperStatusChanged(beforeStatus, afterStatus, moneyOper)

        changeBalanceValueHandler.handler(event)

        verify(domainEventPublisher, times(0)).publish(balance)
    }

    companion object {
        @JvmStatic
        fun paramsFor_handler_balanceIsChanged(): Stream<Arguments>? {
            val expected1 = BigDecimal("101.00")
            val expected2 = BigDecimal("99.00")
            return Stream.of(
                arguments(Pending, Done, expected1),
                arguments(Cancelled, Done, expected1),
                arguments(New, Done, expected1),
                arguments(Done, Pending, expected2),
                arguments(Done, Cancelled, expected2)
            )
        }

        @JvmStatic
        fun paramsFor_handler_balanceNotChanged(): Stream<Arguments>? {
            return Stream.of(
                arguments(Pending, Cancelled),
                arguments(Cancelled, Pending),
                arguments(New, Cancelled),
                arguments(New, Pending),
                arguments(Recurrence, Pending)
            )
        }
    }

}