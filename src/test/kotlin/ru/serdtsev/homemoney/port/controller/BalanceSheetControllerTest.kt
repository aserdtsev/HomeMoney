package ru.serdtsev.homemoney.port.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.test.util.ReflectionTestUtils
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.AnnuityPayment
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.account.Credit
import ru.serdtsev.homemoney.domain.model.balancesheet.CategoryStat
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.port.common.localDateToLong
import ru.serdtsev.homemoney.port.dto.balancesheet.BsDayStatDto
import ru.serdtsev.homemoney.port.dto.balancesheet.BsStatDto
import ru.serdtsev.homemoney.port.dto.balancesheet.CategoryStatDto
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.stream.IntStream
import kotlin.streams.asSequence

internal class BalanceSheetControllerTest : SpringBootBaseTest() {
    @Autowired @Qualifier("conversionService")
    private lateinit var conversionService: ConversionService
    @Autowired
    private lateinit var balanceSheetController: BalanceSheetController

    private lateinit var salaryTag: Tag
    private lateinit var foodstuffsTag: Tag

    private lateinit var debitCard: Balance
    private lateinit var creditCard: Balance

    @BeforeEach
    internal fun setUp() {
        ReflectionTestUtils.setField(balanceSheetController, "clock", clock)

        salaryTag = Tag.of("Зарплата", CategoryType.income)
        foodstuffsTag = Tag.of("Продукты", CategoryType.expense)

        debitCard = Balance(AccountType.debit, "Дебетовая карта", BigDecimal("100000.00"))
            .apply { domainEventPublisher.publish(this) }
        creditCard = run {
            val creditParams = Credit(BigDecimal("400000.00"), 12, 6)
            Balance(AccountType.debit, "Кредитная карта", credit = creditParams)
                .apply { domainEventPublisher.publish(this) }
        }
    }

    @Test
    internal fun `getBsStat by single income to debit card`() {
        val currentDate = LocalDate.now(clock)
        val interval = 1L

        val m1Date = currentDate.minusDays(1)
        MoneyOper(MoneyOperStatus.Done, m1Date, mutableListOf(salaryTag), period = Period.Single)
            .apply {
                addItem(debitCard, BigDecimal("100.00"))
                newAndComplete()
            }

        val actual = balanceSheetController.getBalanceSheetInfo(interval).data

        val dayStatM1 = BsDayStatDto(localDateToLong(m1Date), BigDecimal("100100.00"), BigDecimal("100100.00"),
            BigDecimal("100.00"))
        val dayStats = listOf(dayStatM1)

        val expected = BsStatDto(currentDate.minusMonths(interval), currentDate,
            debitSaldo = BigDecimal("100100.00"),
            totalSaldo = BigDecimal("100100.00"),
            freeAmount = BigDecimal("100100.00"),
            incomeAmount = BigDecimal("100.00"),
            dayStats = dayStats)
        assertThat(actual)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    @Test
    internal fun `getBsStat by recurrence income to debit card`() {
        val currentDate = LocalDate.now(clock)
        val interval = 1L

        val m1Date = currentDate.minusDays(1)
        val p1Date = currentDate.plusMonths(interval).minusDays(1L)
        MoneyOper(MoneyOperStatus.Done, m1Date, mutableListOf(salaryTag), period = Period.Month)
            .apply {
                addItem(debitCard, BigDecimal("100000.00"))
                newAndComplete()
                RecurrenceOper.of(this)
            }

        val actual = balanceSheetController.getBalanceSheetInfo(interval).data

        val dayStatM1 = BsDayStatDto(localDateToLong(m1Date),
            totalSaldo = BigDecimal("200000.00"),
            freeAmount = BigDecimal("200000.00"),
            incomeAmount = BigDecimal("100000.00"))
        val dayStatP1 = BsDayStatDto(localDateToLong(p1Date),
            totalSaldo = BigDecimal("300000.00"),
            freeAmount = BigDecimal("300000.00"),
            incomeAmount = BigDecimal("100000.00"))
        val dayStats = listOf(dayStatM1, dayStatP1)

        val expected = BsStatDto(currentDate.minusMonths(interval), currentDate,
            debitSaldo = BigDecimal("200000.00"),
            totalSaldo = BigDecimal("200000.00"),
            freeAmount = BigDecimal("200000.00"),
            incomeAmount = BigDecimal("100000.00"),
            dayStats = dayStats)
        assertThat(actual)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    @Test
    internal fun `getBsStat by simple charge from debit card`() {
        val currentDate = LocalDate.now(clock)
        val interval = 1L

        val m1Date = currentDate.minusDays(1)
        MoneyOper(MoneyOperStatus.Done, m1Date, mutableListOf(foodstuffsTag), period = Period.Month,
            comment = "Продукты, дебетовая карта")
            .apply {
                addItem(debitCard, BigDecimal("-100.00"))
                newAndComplete()
            }

        val actual = balanceSheetController.getBalanceSheetInfo(interval).data

        val dayStatM1 = BsDayStatDto(localDateToLong(m1Date),
            totalSaldo = BigDecimal("99900.00"),
            freeAmount = BigDecimal("99900.00"),
            chargeAmount = BigDecimal("100.00"))
        val dayStats = listOf(dayStatM1)

        val categories = run {
            val foodstuffsCategoryStatDto = requireNotNull(conversionService.convert(CategoryStat.of(foodstuffsTag,
                BigDecimal("100.00")), CategoryStatDto::class.java))
            listOf(foodstuffsCategoryStatDto)
        }
        val expected = BsStatDto(currentDate.minusMonths(interval), currentDate,
            debitSaldo = BigDecimal("99900.00"),
            totalSaldo = BigDecimal("99900.00"),
            freeAmount = BigDecimal("99900.00"),
            chargesAmount = BigDecimal("100.00"),
            categories = categories,
            dayStats = dayStats)
        assertThat(actual)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    @Test
    internal fun `getBsStat by simple charge from credit card`() {
        val currentDate = LocalDate.parse("2023-09-03")
        setCurrentDate(currentDate)

        val m2Date = currentDate.minusDays(2)
        val m1Date = currentDate.minusDays(1)

        val moneyOper = MoneyOper(MoneyOperStatus.Done, m2Date, mutableListOf(foodstuffsTag), period = Period.Single)
            .apply {
                addItem(creditCard, BigDecimal("-100.00"))
                newAndComplete()
            }
        val p1Date = moneyOper.items[0].dateWithGracePeriod
        val interval = 2L

        MoneyOper(MoneyOperStatus.Done, m1Date, mutableListOf(foodstuffsTag), period = Period.Single)
            .apply {
                addItem(creditCard, BigDecimal("-100.00"))
                newAndComplete()
                assert(items[0].dateWithGracePeriod == p1Date)
            }

        val actual = balanceSheetController.getBalanceSheetInfo(interval).data

        val dayStatM2 = BsDayStatDto(localDateToLong(m2Date),
            totalSaldo = BigDecimal("99900.00"),
            freeAmount = BigDecimal("100000.00"),
            chargeAmount = BigDecimal("100.00"))
        val dayStatM1 = BsDayStatDto(localDateToLong(m1Date),
            totalSaldo = BigDecimal("99800.00"),
            freeAmount = BigDecimal("100000.00"),
            chargeAmount = BigDecimal("100.00"))
        val dayStatP1 = BsDayStatDto(localDateToLong(p1Date),
            totalSaldo = BigDecimal("99800.00"),
            freeAmount = BigDecimal("99800.00"),
            debt = BigDecimal("0.00"))
        val dayStats = listOf(dayStatM2, dayStatM1, dayStatP1)

        val categories = run {
            val foodstuffsCategoryStat = requireNotNull(conversionService.convert(CategoryStat.of(foodstuffsTag,
                BigDecimal("200.00")), CategoryStatDto::class.java))
            listOf(foodstuffsCategoryStat)
        }
        val expected = BsStatDto(currentDate.minusMonths(interval), currentDate,
            debitSaldo = BigDecimal("99800.00"),
            totalSaldo = BigDecimal("99800.00"),
            chargesAmount = BigDecimal("200.00"),
            freeAmount = BigDecimal("100000.00"),
            currentCreditCardDebt = BigDecimal("200.00"),
            categories = categories,
            dayStats = dayStats)
        assertThat(actual)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    @Test
    internal fun `getBsStat by before interval charge from credit card`() {
        val currentDate = LocalDate.parse("2023-09-05")
        setCurrentDate(currentDate)

        val m1Date = LocalDate.parse("2023-08-04")

        val moneyOper = MoneyOper(MoneyOperStatus.Done, m1Date, mutableListOf(foodstuffsTag), period = Period.Single)
            .apply {
                addItem(creditCard, BigDecimal("-100.00"))
                newAndComplete()
            }
        val p1Date = moneyOper.items[0].dateWithGracePeriod
        val interval = 1L

        val actual = balanceSheetController.getBalanceSheetInfo(interval).data

        val dayStatP1 = BsDayStatDto(localDateToLong(p1Date),
            totalSaldo = BigDecimal("99900.00"),
            freeAmount = BigDecimal("99900.00"))
        val dayStats = listOf(dayStatP1)

        val expected = BsStatDto(currentDate.minusMonths(interval), currentDate,
            debitSaldo = BigDecimal("99900.00"),
            totalSaldo = BigDecimal("99900.00"),
            freeAmount = BigDecimal("100000.00"),
            currentCreditCardDebt = BigDecimal("100.00"),
            dayStats = dayStats)
        assertThat(actual)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    @Test
    internal fun `getBsStat by early repayment of credit card debt`() {
        val currentDate = LocalDate.parse("2023-09-02")
        setCurrentDate(currentDate)

        val interval = 2L
        val m1Date = currentDate.minusMonths(1)

        MoneyOper(MoneyOperStatus.Done, m1Date, mutableListOf(foodstuffsTag), period = Period.Single)
            .apply {
                addItem(creditCard, BigDecimal("-100.00"))
                newAndComplete()
            }
        MoneyOper(MoneyOperStatus.Done, currentDate, period = Period.Single)
            .apply {
                addItem(debitCard, BigDecimal("-100.00"))
                addItem(creditCard, BigDecimal("100.00"))
                newAndComplete()
            }

        val actual = balanceSheetController.getBalanceSheetInfo(interval).data

        val dayStatM1 = BsDayStatDto(localDateToLong(m1Date),
            totalSaldo = BigDecimal("99900.00"),
            freeAmount = BigDecimal("100000.00"),
            chargeAmount = BigDecimal("100.00"))
        val dayStatC = BsDayStatDto(localDateToLong(currentDate),
            totalSaldo = BigDecimal("99900.00"),
            freeAmount = BigDecimal("99900.00"))

        val dayStats = listOf(dayStatM1, dayStatC)

        val categories = run {
            val foodstuffsCategoryStat = requireNotNull(conversionService.convert(CategoryStat.of(foodstuffsTag,
                BigDecimal("100.00")), CategoryStatDto::class.java))
            listOf(foodstuffsCategoryStat)
        }
        val expected = BsStatDto(currentDate.minusMonths(interval), currentDate,
            debitSaldo = BigDecimal("99900.00"),
            totalSaldo = BigDecimal("99900.00"),
            chargesAmount = BigDecimal("100.00"),
            freeAmount = BigDecimal("99900.00"),
            currentCreditCardDebt = BigDecimal("0.00"),
            categories = categories,
            dayStats = dayStats)
        assertThat(actual)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    @Test
    internal fun `getBsStat by recurrence debt repayment from debit card`() {
        val credit = run {
            val annuityPayment = AnnuityPayment(BigDecimal("25000.00"))
            val creditParams = Credit(annuityPayment = annuityPayment)
            Balance(AccountType.credit, "Кредит", BigDecimal("-100000.00"), credit = creditParams)
                .apply { domainEventPublisher.publish(this) }
        }

        val currentDate = LocalDate.now()
        val interval = 1L

        val m1Date = currentDate.minusDays(1)
        val p1Date = m1Date.plusMonths(interval)
        MoneyOper(MoneyOperStatus.Done, m1Date, period = Period.Month)
            .apply {
                addItem(debitCard, BigDecimal("-25000.00"))
                addItem(credit, BigDecimal("25000.00"))
                newAndComplete()
                RecurrenceOper.of(this)
            }

        val actual = balanceSheetController.getBalanceSheetInfo(interval).data

        val dayStatM1 = BsDayStatDto(localDateToLong(m1Date), freeAmount = BigDecimal("75000.00"))
        val dayStatP1 = BsDayStatDto(localDateToLong(p1Date), freeAmount = BigDecimal("50000.00"))
        val dayStats = listOf(dayStatM1, dayStatP1)

        val expected = BsStatDto(currentDate.minusMonths(interval), currentDate,
            debitSaldo = BigDecimal("75000.00"),
            creditSaldo = BigDecimal("-75000.00"),
            freeAmount = BigDecimal("75000.00"),
            actualDebt = BigDecimal("-25000.00"),
            dayStats = dayStats)
        assertThat(actual)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    @Test
    internal fun `getBsStat by recurrence charge from credit card`() {
        val currentDate = LocalDate.parse("2023-09-02")
        setCurrentDate(currentDate)
        val m1Date = currentDate.minusDays(1)
        val moneyOper = MoneyOper(MoneyOperStatus.Done, m1Date, mutableListOf(foodstuffsTag), period = Period.Month)
            .apply {
                addItem(creditCard, BigDecimal("-100.00"))
                newAndComplete()
            }
        val recurrenceOper = RecurrenceOper.of(moneyOper)
        val p1Date = recurrenceOper.nextDate
        val p2Date = moneyOper.items[0].dateWithGracePeriod
        val p3Date = recurrenceOper.nextDate.plusMonths(1)
        val interval = 2L

        val actual = balanceSheetController.getBalanceSheetInfo(interval).data

        val dayStatM1 = BsDayStatDto(localDateToLong(m1Date),
            totalSaldo = BigDecimal("99900.00"),
            freeAmount = BigDecimal("100000.00"),
            chargeAmount = BigDecimal("100.00"))
        val dayStatP1 = BsDayStatDto(localDateToLong(p1Date),
            totalSaldo = BigDecimal("99800.00"),
            freeAmount = BigDecimal("100000.00"),
            chargeAmount = BigDecimal("100.00"))
        val dayStatP2 = BsDayStatDto(localDateToLong(p2Date),
            totalSaldo = BigDecimal("99800.00"),
            freeAmount = BigDecimal("99900.00"))
        val dayStatP3 = BsDayStatDto(localDateToLong(p3Date),
            totalSaldo = BigDecimal("99700.00"),
            freeAmount = BigDecimal("99900.00"),
            chargeAmount = BigDecimal("100.00"))
        val dayStats = listOf(dayStatM1, dayStatP1, dayStatP2, dayStatP3)

        val categories = run {
            val foodstuffsCategoryStat = CategoryStat.of(foodstuffsTag, BigDecimal("100.00"))
            listOf(requireNotNull(conversionService.convert(foodstuffsCategoryStat, CategoryStatDto::class.java)))
        }
        val expected = BsStatDto(currentDate.minusMonths(interval), currentDate,
            debitSaldo = BigDecimal("99900.00"),
            totalSaldo = BigDecimal("99900.00"),
            freeAmount = BigDecimal("100000.00"),
            chargesAmount = BigDecimal("100.00"),
            categories = categories,
            currentCreditCardDebt = BigDecimal("100.00"),
            dayStats = dayStats)
        assertThat(actual)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    @Test
    internal fun `getBsStat by several operations`() {
        val currentDate = LocalDate.now(clock)
        val interval = 1L

        val m1Date = currentDate.minusDays(1)
        val p1Date = currentDate.plusDays(1)
        MoneyOper(MoneyOperStatus.Done, m1Date, mutableListOf(salaryTag), period = Period.Single)
            .apply {
                addItem(debitCard, BigDecimal("100.00"))
                newAndComplete()
            }
        MoneyOper(MoneyOperStatus.Done, m1Date, mutableListOf(foodstuffsTag), period = Period.Month)
            .apply {
                addItem(debitCard, BigDecimal("-100.00"))
                newAndComplete()
            }
        MoneyOper(MoneyOperStatus.Pending, p1Date, mutableListOf(foodstuffsTag), period = Period.Single)
            .apply {
                addItem(debitCard, BigDecimal("-100.00"))
                domainEventPublisher.publish(this)
            }
        val categories = run {
            val foodstuffsCategoryStatDto = requireNotNull(conversionService.convert(CategoryStat.of(foodstuffsTag,
                BigDecimal("100.00")), CategoryStatDto::class.java))
            listOf(foodstuffsCategoryStatDto)
        }

        val actual = balanceSheetController.getBalanceSheetInfo(interval).data

        val dayStatM1 = BsDayStatDto(localDateToLong(m1Date), BigDecimal("100000.00"), BigDecimal("100000.00"),
            BigDecimal("100.00"), BigDecimal("100.00"))
        val dayStatP1 = BsDayStatDto(localDateToLong(p1Date), BigDecimal("99900.00"), BigDecimal("99900.00"),
            BigDecimal("0.00"), BigDecimal("100.00"))
        val dayStats = listOf(dayStatM1, dayStatP1)

        val expected = BsStatDto(currentDate.minusMonths(interval), currentDate,
            debitSaldo = BigDecimal("100000.00"),
            totalSaldo = BigDecimal("100000.00"),
            freeAmount = BigDecimal("100000.00"),
            incomeAmount = BigDecimal("100.00"),
            chargesAmount = BigDecimal("100.00"),
            categories = categories,
            dayStats = dayStats)
        assertThat(actual)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    @Test
    internal fun `getBsStat by trend operation`() {
        var currentDate = LocalDate.parse("2024-01-01").apply { setCurrentDate(this) }

        val interval = 1L

        val dates = IntStream.iterate(14) { it + 14 }
            .limit(3)
            .asLongStream()
            .asSequence()
            .map { currentDate.plusDays(it) }
        dates.forEach {
            MoneyOper(MoneyOperStatus.Done, it, mutableListOf(foodstuffsTag),
                period = Period.Day, comment = "Продукты, дебетовая карта")
                .apply {
                    addItem(debitCard, BigDecimal("-100.00"))
                    newAndComplete()
                }
        }

        currentDate = LocalDate.parse("2024-02-12").apply { setCurrentDate(this) }

        val actual = balanceSheetController.getBalanceSheetInfo(interval).data

        val dayStatM1 = BsDayStatDto(localDateToLong(LocalDate.parse("2024-01-15")),
            totalSaldo = BigDecimal("99900.00"),
            freeAmount = BigDecimal("99900.00"),
            chargeAmount = BigDecimal("100.00"))
        val dayStatM2 = BsDayStatDto(localDateToLong(LocalDate.parse("2024-01-29")),
            totalSaldo = BigDecimal("99800.00"),
            freeAmount = BigDecimal("99800.00"),
            chargeAmount = BigDecimal("100.00"))
        val dayStatP1 = BsDayStatDto(localDateToLong(LocalDate.parse("2024-02-12")),
            totalSaldo = BigDecimal("99700.00"),
            freeAmount = BigDecimal("99700.00"),
            chargeAmount = BigDecimal("100.00"))
        val dayStatP2 = BsDayStatDto(localDateToLong(LocalDate.parse("2024-02-26")),
            totalSaldo = BigDecimal("99600.00"),
            freeAmount = BigDecimal("99600.00"),
            chargeAmount = BigDecimal("100.00"))
        val dayStatP3 = BsDayStatDto(localDateToLong(LocalDate.parse("2024-03-11")),
            totalSaldo = BigDecimal("99500.00"),
            freeAmount = BigDecimal("99500.00"),
            chargeAmount = BigDecimal("100.00"))

        val dayStats = listOf(dayStatM1, dayStatM2, dayStatP1, dayStatP2, dayStatP3)

        val categories = run {
            val foodstuffsCategoryStatDto = requireNotNull(conversionService.convert(CategoryStat.of(foodstuffsTag,
                BigDecimal("300.00")), CategoryStatDto::class.java))
            listOf(foodstuffsCategoryStatDto)
        }
        val expected = BsStatDto(currentDate.minusMonths(interval), currentDate,
            debitSaldo = BigDecimal("99700.00"),
            totalSaldo = BigDecimal("99700.00"),
            freeAmount = BigDecimal("99700.00"),
            chargesAmount = BigDecimal("300.00"),
            categories = categories,
            dayStats = dayStats)
        assertThat(actual)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }

    private fun setCurrentDate(currentDate: LocalDate) {
        val zoneId = ZoneId.systemDefault()
        clock = Clock.fixed(currentDate.atStartOfDay(zoneId).toInstant(), zoneId)
        ReflectionTestUtils.setField(balanceSheetController, "clock", clock)
    }

}