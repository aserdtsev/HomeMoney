package ru.serdtsev.homemoney.domain.event

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.domain.DomainBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus.Done
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus.Trend
import ru.serdtsev.homemoney.domain.repository.BalanceRepository
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.stream.IntStream
import kotlin.streams.asSequence

internal class CreateOrUpdateTrendMoneyOperHandlerTest: DomainBaseTest() {
    private lateinit var handler: CreateOrUpdateTrendMoneyOperHandler
    private lateinit var moneyOperRepository: MoneyOperRepository
    private lateinit var balanceRepository: BalanceRepository
    private lateinit var clock: Clock
    private lateinit var card: Balance
    private lateinit var salaryTag: Tag
    private lateinit var foodstuffsTag: Tag

    @BeforeEach
    override fun setUp() {
        super.setUp()
        moneyOperRepository = repositoryRegistry.moneyOperRepository
        balanceRepository = repositoryRegistry.balanceRepository
        clock = run {
            val currentDate = LocalDate.parse("2024-01-01")
            Clock.fixed(currentDate.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        }
        handler = CreateOrUpdateTrendMoneyOperHandler(moneyOperRepository, balanceRepository, clock)
        card = Balance(AccountType.debit, "Дебетовая карта", BigDecimal("100000.00"))
        salaryTag = Tag.of("Зарплата", CategoryType.income)
        foodstuffsTag = Tag.of("Продукты", CategoryType.expense)
    }

    @Test
    fun handler() {
        val num = 3L
        val initDate = LocalDate.now(clock).minusDays(num * 2)
        val dates = IntStream.iterate(1) { it + 1 }
            .limit(num)
            .asLongStream()
            .asSequence()
            .map { initDate.plusDays(it) }
        val moneyOpers = dates
            .map {
                MoneyOper(Done, it, mutableListOf(foodstuffsTag), period = Period.Day)
                    .apply { addItem(card, BigDecimal("-100.00")) }
            }
            .toList()
        val lastMoneyOper = moneyOpers.last()
        val event = MoneyOperStatusChanged(MoneyOperStatus.New, Done, lastMoneyOper)

        run {
            val startDate = LocalDate.now(clock).minusDays(30)
            whenever(moneyOperRepository.findByStatusAndPerformedGreaterThan(Done, startDate))
                .thenReturn(moneyOpers)
        }
        whenever(moneyOperRepository.findTrend(foodstuffsTag, Period.Day)).thenReturn(null)
        whenever(balanceRepository.findById(card.id)).thenReturn(card)

        doAnswer {
            val actual = it.arguments[0] as MoneyOper
            assertThat(actual)
                .extracting("status", "performed", "tags", "recurrenceParams", "items")
                .contains(Trend, LocalDate.now(clock), setOf(foodstuffsTag), DayRecurrenceParams(2))
        }.whenever(domainEventPublisher).publish(any<MoneyOper>())

        handler.moneyOperStatusChangedHandlerHandler(event)

        verify(domainEventPublisher).publish(any<MoneyOper>())
    }
}