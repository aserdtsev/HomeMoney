package ru.serdtsev.homemoney.port.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.AnnuityPayment
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
            val creditParams = Credit(BigDecimal("400000.00"), 12, gracePeriodDays)
            Balance(AccountType.debit, "Кредитная карта", credit = creditParams)
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

        val actual = statService.getBsStat(currentDate, interval)

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

        val actual = statService.getBsStat(currentDate, interval)

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

        val actual = statService.getBsStat(currentDate, interval)

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
        val currentDate = LocalDate.parse("2023-09-03")

        val m2Date = currentDate.minusDays(2)
        val m1Date = currentDate.minusDays(1)
        val p1Date = currentDate.plusDays(1)
        val p2Date = currentDate.plusDays(2)

        val moneyOper = MoneyOper(MoneyOperStatus.doneNew, m2Date, mutableListOf(foodstuffsTag), period = Period.month)
            .apply {
                addItem(creditCard, BigDecimal("-100.00"))
                complete()
            }
        val p3Date = moneyOper.items[0].dateWithGracePeriod
        val interval = ChronoUnit.DAYS.between(currentDate, p3Date)

        MoneyOper(MoneyOperStatus.doneNew, m1Date, mutableListOf(foodstuffsTag), period = Period.month)
            .apply {
                addItem(creditCard, BigDecimal("-100.00"))
                complete()
                assert(items[0].dateWithGracePeriod == p3Date)
            }

        val actual = statService.getBsStat(currentDate, interval)

        val dayStatM2 = BsDayStat(m2Date, chargeAmount = BigDecimal("100.00"), freeCorrection = BigDecimal("100.00"))
        val dayStatM1 = BsDayStat(m1Date, chargeAmount = BigDecimal("100.00"), freeCorrection = BigDecimal("200.00"))
        val dayStatP1 = BsDayStat(p1Date, chargeAmount = BigDecimal("100.00"), freeCorrection = BigDecimal("300.00"))
        val dayStatP2 = BsDayStat(p2Date, chargeAmount = BigDecimal("100.00"), freeCorrection = BigDecimal("400.00"))
        val dayStatP3 = BsDayStat(p3Date, debt = BigDecimal("0.00"))
        val dayStats: List<BsDayStat> = listOf(dayStatM2, dayStatM1, dayStatP1, dayStatP2, dayStatP3)

        val categories = run {
            val foodstuffsCategoryStat = CategoryStat.of(foodstuffsTag, BigDecimal("200.00"))
            listOf(foodstuffsCategoryStat)
        }
        val expected = BsStat(currentDate.minusDays(interval), currentDate, chargesAmount = BigDecimal("200.00"),
            dayStats = dayStats, categories = categories, actualCreditCardDebt = BigDecimal("-200.00"))
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("saldoMap", "dayStats.deltaMap", "dayStats.saldoMap")
            .isEqualTo(expected)
        assertEquals(BigDecimal("99800.00"), actual.totalSaldo)
        assertEquals(BigDecimal("99800.00"), actual.debitSaldo)
        assertEquals(BigDecimal.ZERO, actual.reserveSaldo)
        assertEquals(BigDecimal.ZERO, actual.creditSaldo)
        assertEquals(BigDecimal.ZERO, actual.assetSaldo)
        assertEquals(BigDecimal("100000.00"), actual.freeAmount)
        actual.dayStats
            .forEach {
                when (it) {
                    dayStatM2 -> {
                        assertEquals(BigDecimal("99900.00"), it.totalSaldo)
                        assertEquals(BigDecimal("100000.00"), it.freeAmount)
                    }
                    dayStatM1 -> {
                        assertEquals(BigDecimal("99800.00"), it.totalSaldo)
                        assertEquals(BigDecimal("100000.00"), it.freeAmount)
                    }
                    dayStatP1 -> {
                        assertEquals(BigDecimal("99700.00"), it.totalSaldo)
                        assertEquals(BigDecimal("100000.00"), it.freeAmount)
                    }
                    dayStatP2 -> {
                        assertEquals(BigDecimal("99600.00"), it.totalSaldo)
                        assertEquals(BigDecimal("100000.00"), it.freeAmount)
                    }
                    dayStatP3 -> {
                        assertEquals(BigDecimal("99600.00"), it.totalSaldo)
                        assertEquals(BigDecimal("99600.00"), it.freeAmount)
                    }
                }
            }
    }

    @Test
    internal fun `getBsStat by recurrence debt repayment`() {
        val credit = run {
            val annuityPayment = AnnuityPayment(BigDecimal("25000.00"))
            val creditParams = Credit(annuityPayment = annuityPayment)
            Balance(AccountType.credit, "Кредит", BigDecimal("-100000.00"), credit = creditParams)
                .apply { domainEventPublisher.publish(this) }
        }

        val currentDate = LocalDate.now()
        val interval = ChronoUnit.DAYS.between(currentDate, currentDate.plusMonths(1L))

        val m1Date = currentDate.minusDays(1)
        val p1Date = currentDate.plusDays(interval).minusDays(1L)
        MoneyOper(MoneyOperStatus.doneNew, m1Date, period = Period.month)
            .apply {
                addItem(debitCard, BigDecimal("-25000.00"))
                addItem(credit, BigDecimal("25000.00"))
                complete()
                RecurrenceOper.of(this)
            }

        val actual = statService.getBsStat(currentDate, interval)

        val dayStatM1 = BsDayStat(m1Date)
        val dayStatP1 = BsDayStat(p1Date)
        val dayStats: List<BsDayStat> = listOf(dayStatM1, dayStatP1)

        val expected = BsStat(currentDate.minusDays(interval), currentDate, dayStats = dayStats,
            actualDebt = BigDecimal("-25000.00"))
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("saldoMap", "dayStats.deltaMap", "dayStats.saldoMap")
            .isEqualTo(expected)
        assertEquals(BigDecimal("0.00"), actual.totalSaldo)
        assertEquals(BigDecimal("75000.00"), actual.debitSaldo)
        assertEquals(BigDecimal.ZERO, actual.reserveSaldo)
        assertEquals(BigDecimal("-75000.00"), actual.creditSaldo)
        assertEquals(BigDecimal.ZERO, actual.assetSaldo)
        assertEquals(BigDecimal("75000.00"), actual.freeAmount)
        actual.dayStats
            .forEach {
                when (it) {
                    dayStatM1 -> {
                        assertEquals(BigDecimal("0.00"), it.totalSaldo)
                        assertEquals(BigDecimal("75000.00"), it.freeAmount)
                    }

                    dayStatP1 -> {
                        assertEquals(BigDecimal("0.00"), it.totalSaldo)
                        assertEquals(BigDecimal("50000.00"), it.freeAmount)
                    }
                }
            }
    }

}