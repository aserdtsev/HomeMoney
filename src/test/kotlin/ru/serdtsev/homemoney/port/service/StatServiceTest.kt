package ru.serdtsev.homemoney.port.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.account.Credit
import ru.serdtsev.homemoney.domain.model.balancesheet.BsDayStat
import ru.serdtsev.homemoney.domain.model.balancesheet.BsStat
import ru.serdtsev.homemoney.domain.model.balancesheet.CategoryStat
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal class StatServiceTest : SpringBootBaseTest() {
    @Autowired
    private lateinit var statService: StatService

    private lateinit var salaryTag: Tag
    private lateinit var foodstuffsTag: Tag

    private lateinit var debitCard: Balance
    private lateinit var creditCard: Balance

    private final val gracePeriodDays = 55

    @BeforeEach
    internal fun setUp() {
        salaryTag = Tag.of("Зарплата", CategoryType.income)
        foodstuffsTag = Tag.of("Продукты", CategoryType.expense)

        debitCard = Balance(AccountType.debit, "Дебетовая карта", BigDecimal("100000.00")).apply {
            domainEventPublisher.publish(this)
        }
        creditCard = run {
            val credit = Credit(BigDecimal("400000.00"), 12, gracePeriodDays)
            Balance(AccountType.debit, "Кредитная карта", credit = credit)
                .apply { domainEventPublisher.publish(this) }
        }
    }

    @Test
    internal fun `getBsStat by single income to debit card`() {
        val currentDate = LocalDate.now()
        val interval = 1L

        val m1Date = currentDate.minusDays(1)
        MoneyOper(MoneyOperStatus.doneNew, m1Date, mutableListOf(foodstuffsTag), period = Period.single)
            .apply {
                addItem(debitCard, BigDecimal("100.00"))
                complete()
            }

        val actual = statService.getBsStat(interval)

        val dayStatM1 = BsDayStat(m1Date, BigDecimal("100.00"))
        val dayStats: List<BsDayStat> = listOf(dayStatM1)

        val expected = BsStat(currentDate.minusDays(interval), currentDate, incomeAmount = BigDecimal("100.00"),
            dayStats = dayStats)
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("saldoMap", "dayStats.deltaMap", "dayStats.saldoMap")
            .isEqualTo(expected)
        assertEquals(BigDecimal("100100.00"), actual.totalSaldo)
        assertEquals(BigDecimal("100100.00"), actual.debitSaldo)
        assertEquals(BigDecimal.ZERO, actual.reserveSaldo)
        assertEquals(BigDecimal.ZERO, actual.creditSaldo)
        assertEquals(BigDecimal.ZERO, actual.assetSaldo)
        assertEquals(BigDecimal("100100.00"), actual.freeAmount)
        actual.dayStats
            .forEach {
                when (it) {
                    dayStatM1 -> {
                        assertEquals(BigDecimal("100100.00"), it.totalSaldo)
                        assertEquals(BigDecimal("100100.00"), it.freeAmount)
                    }
                }
            }
    }

    @Test
    internal fun `getBsStat by recurrence income to debit card`() {
        val currentDate = LocalDate.now()
        val interval = ChronoUnit.DAYS.between(currentDate, currentDate.plusMonths(1L))

        val m1Date = currentDate.minusDays(1)
        val p1Date = currentDate.plusDays(interval).minusDays(1L)
        MoneyOper(MoneyOperStatus.doneNew, m1Date, mutableListOf(salaryTag), period = Period.month)
            .apply {
                addItem(debitCard, BigDecimal("100000.00"))
                complete()
                RecurrenceOper.of(this)
            }

        val actual = statService.getBsStat(interval)

        val dayStatM1 = BsDayStat(m1Date, incomeAmount = BigDecimal("100000.00"))
        val dayStatP1 = BsDayStat(p1Date, incomeAmount = BigDecimal("100000.00"))
        val dayStats: List<BsDayStat> = listOf(dayStatM1, dayStatP1)

        val expected = BsStat(currentDate.minusDays(interval), currentDate, incomeAmount = BigDecimal("100000.00"),
            dayStats = dayStats)
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("saldoMap", "dayStats.deltaMap", "dayStats.saldoMap")
            .isEqualTo(expected)
        assertEquals(BigDecimal("200000.00"), actual.totalSaldo)
        assertEquals(BigDecimal("200000.00"), actual.debitSaldo)
        assertEquals(BigDecimal.ZERO, actual.reserveSaldo)
        assertEquals(BigDecimal.ZERO, actual.creditSaldo)
        assertEquals(BigDecimal.ZERO, actual.assetSaldo)
        assertEquals(BigDecimal("200000.00"), actual.freeAmount)
        actual.dayStats
            .forEach {
                when (it) {
                    dayStatM1 -> {
                        assertEquals(BigDecimal("200000.00"), it.totalSaldo)
                        assertEquals(BigDecimal("200000.00"), it.freeAmount)
                    }

                    dayStatP1 -> {
                        assertEquals(BigDecimal("300000.00"), it.totalSaldo)
                        assertEquals(BigDecimal("300000.00"), it.freeAmount)
                    }
                }
            }
    }

    @Test
    internal fun `getBsStat by simple charge from debit card`() {
        val currentDate = LocalDate.now()
        val interval = 1L

        val m1Date = currentDate.minusDays(1)
        val p1Date = currentDate.plusDays(1)
        MoneyOper(MoneyOperStatus.doneNew, m1Date, mutableListOf(foodstuffsTag), period = Period.month,
            comment = "Продукты, дебетовая карта")
            .apply {
                addItem(debitCard, BigDecimal("-100.00"))
                complete()
            }

        val actual = statService.getBsStat(interval)

        val dayStatM1 = BsDayStat(m1Date, chargeAmount = BigDecimal("100.00"))
        val dayStatP1 = BsDayStat(p1Date, chargeAmount = BigDecimal("100.00"))
        val dayStats: List<BsDayStat> = listOf(dayStatM1, dayStatP1)

        val categories = run {
            val foodstuffsCategoryStat = CategoryStat.of(foodstuffsTag, BigDecimal("100.00"))
            listOf(foodstuffsCategoryStat)
        }
        val expected = BsStat(currentDate.minusDays(interval), currentDate, chargesAmount = BigDecimal("100.00"),
            dayStats = dayStats, categories = categories)
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("saldoMap", "dayStats.deltaMap", "dayStats.saldoMap")
            .isEqualTo(expected)
        assertEquals(BigDecimal("99900.00"), actual.totalSaldo)
        assertEquals(BigDecimal("99900.00"), actual.debitSaldo)
        assertEquals(BigDecimal.ZERO, actual.reserveSaldo)
        assertEquals(BigDecimal.ZERO, actual.creditSaldo)
        assertEquals(BigDecimal.ZERO, actual.assetSaldo)
        assertEquals(BigDecimal("99900.00"), actual.freeAmount)
        actual.dayStats
            .forEach {
                when (it) {
                    dayStatM1 -> {
                        assertEquals(BigDecimal("99900.00"), it.totalSaldo)
                        assertEquals(BigDecimal("99900.00"), it.freeAmount)
                    }

                    dayStatP1 -> {
                        assertEquals(BigDecimal("99800.00"), it.totalSaldo)
                        assertEquals(BigDecimal("99800.00"), it.freeAmount)
                    }
                }
            }
    }

    @Test
    internal fun `getBsStat by simple charge from credit card`() {
        val currentDate = LocalDate.now()

        val m1Date = currentDate.minusDays(1)
        val p1Date = currentDate.plusDays(1)
        val moneyOper = MoneyOper(MoneyOperStatus.doneNew, m1Date, mutableListOf(foodstuffsTag), period = Period.month)
            .apply {
                addItem(creditCard, BigDecimal("-100.00"))
                complete()
            }
        val p2Date = moneyOper.items[0].dateWithGracePeriod
        val interval = ChronoUnit.DAYS.between(currentDate, p2Date)

        val actual = statService.getBsStat(interval)

        val dayStatM1 = BsDayStat(m1Date, chargeAmount = BigDecimal("100.00"), debt = BigDecimal("100.00"))
        val dayStatP1 = BsDayStat(p1Date, chargeAmount = BigDecimal("100.00"), debt = BigDecimal("200.00"))
        val dayStatP2 = BsDayStat(p2Date, debt = BigDecimal("0.00"))
        val dayStats: List<BsDayStat> = listOf(dayStatM1, dayStatP1, dayStatP2)

        val categories = run {
            val foodstuffsCategoryStat = CategoryStat.of(foodstuffsTag, BigDecimal("100.00"))
            listOf(foodstuffsCategoryStat)
        }
        val expected = BsStat(currentDate.minusDays(interval), currentDate, chargesAmount = BigDecimal("100.00"),
            dayStats = dayStats, categories = categories, actualDebt = BigDecimal("100.00"))
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("saldoMap", "dayStats.deltaMap", "dayStats.saldoMap")
            .isEqualTo(expected)
        assertEquals(BigDecimal("99900.00"), actual.totalSaldo)
        assertEquals(BigDecimal("99900.00"), actual.debitSaldo)
        assertEquals(BigDecimal.ZERO, actual.reserveSaldo)
        assertEquals(BigDecimal.ZERO, actual.creditSaldo)
        assertEquals(BigDecimal.ZERO, actual.assetSaldo)
        assertEquals(BigDecimal("100000.00"), actual.freeAmount)
        actual.dayStats
            .forEach {
                when (it) {
                    dayStatM1 -> {
                        assertEquals(BigDecimal("99900.00"), it.totalSaldo)
                        assertEquals(BigDecimal("100000.00"), it.freeAmount)
                    }
                    dayStatP1 -> {
                        assertEquals(BigDecimal("99800.00"), it.totalSaldo)
                        assertEquals(BigDecimal("100000.00"), it.freeAmount)
                    }
                    dayStatP2 -> {
                        assertEquals(BigDecimal("99800.00"), it.totalSaldo)
                        assertEquals(BigDecimal("99800.00"), it.freeAmount)
                    }
                }
            }
    }

    @Test
    @Disabled
    internal fun getBsStat() {
        val currentDate = LocalDate.now()
        // todo Расширить до месяца
        val interval = 1L

        val m1Date = currentDate.minusDays(1)
        val p1Date = currentDate.plusDays(1)
        MoneyOper(MoneyOperStatus.done, m1Date, mutableListOf(salaryTag),
            period = Period.month, comment = "Зарплата")
            .apply {
                addItem(debitCard, BigDecimal("100000.00"))
                domainEventPublisher.publish(this)
                RecurrenceOper.of(this)
            }
        MoneyOper(MoneyOperStatus.done, m1Date, mutableListOf(foodstuffsTag),
            period = Period.month, comment = "Продукты, дебетовая карта")
            .apply {
                addItem(debitCard, BigDecimal("-100.00"))
                domainEventPublisher.publish(this)
            }
        MoneyOper(MoneyOperStatus.done, m1Date, mutableListOf(foodstuffsTag),
            period = Period.month, comment = "Продукты, кредитная карта")
            .apply {
                addItem(creditCard, BigDecimal("-50.00"))
                domainEventPublisher.publish(this)
            }
        run {
            val sample = MoneyOper(MoneyOperStatus.done, m1Date.minusMonths(3L), mutableListOf(foodstuffsTag),
                period = Period.month, comment = "Продукты, кредитная карта")
                .apply {
                    addItem(creditCard, BigDecimal("-40.00"))
                    domainEventPublisher.publish(this)
                }
            val recurrenceOper = RecurrenceOper.of(sample)
                .apply {
                    skipNextDate()
                    skipNextDate()
                    DomainEventPublisher.instance.publish(this)
                }
            recurrenceOper.createNextMoneyOper().apply {
                this.status = MoneyOperStatus.done
                DomainEventPublisher.instance.publish(this)
            }
        }
        // todo Добавить повторение
//        MoneyOper(MoneyOperStatus.done, currentDate.minusDays(1),
//            period = Period.month, comment = "Пополнение кредитной карты")
//            .apply {
//                addItem(debitCard, BigDecimal("-200.00"))
//                addItem(creditCard, BigDecimal("200.00"))
//                domainEventPublisher.publish(this)
//            }

        val actual = statService.getBsStat(interval)

        val dayStatM1 = BsDayStat(m1Date, BigDecimal("100000.00"),
            BigDecimal("190.00"), debt = BigDecimal("90.00"))
        val dayStatP1 = BsDayStat(p1Date, BigDecimal.ZERO, BigDecimal("150.00"),
            debt = BigDecimal("100.00"))
        val dayStats: List<BsDayStat> = listOf(dayStatM1, dayStatP1)

        val foodstuffsCategoryStat = CategoryStat.of(foodstuffsTag, BigDecimal("190.00"))
        val categories = listOf(foodstuffsCategoryStat)
        val expected = BsStat(currentDate.minusDays(interval), currentDate, BigDecimal("100000.00"),
            BigDecimal("190.00"), dayStats, categories, actualDebt = BigDecimal("90.00"))
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("saldoMap", "dayStats.deltaMap", "dayStats.saldoMap")
            .isEqualTo(expected)
        assertEquals(BigDecimal("199810.00"), actual.totalSaldo)
        assertEquals(BigDecimal("199810.00"), actual.debitSaldo)
        assertEquals(BigDecimal.ZERO, actual.reserveSaldo)
        assertEquals(BigDecimal.ZERO, actual.creditSaldo)
        assertEquals(BigDecimal.ZERO, actual.assetSaldo)
        assertEquals(BigDecimal("199900.00"), actual.freeAmount)
        actual.dayStats
            .forEach {
                when (it) {
                    dayStatM1 -> {
                        assertEquals(BigDecimal("199810.00"), it.totalSaldo)
                        assertEquals(BigDecimal("199900.00"), it.freeAmount)
                    }

                    dayStatP1 -> {
                        assertEquals(BigDecimal("199700.00"), it.totalSaldo)
                        assertEquals(BigDecimal("199800.00"), it.freeAmount)
                    }
                }
            }
    }

}