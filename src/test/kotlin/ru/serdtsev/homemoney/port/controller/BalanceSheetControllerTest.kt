package ru.serdtsev.homemoney.port.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.account.Credit
import ru.serdtsev.homemoney.domain.model.balancesheet.CategoryStat
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.port.common.localDateToLong
import ru.serdtsev.homemoney.port.dto.balancesheet.BsDayStatDto
import ru.serdtsev.homemoney.port.dto.balancesheet.BsStatDto
import ru.serdtsev.homemoney.port.dto.balancesheet.CategoryStatDto
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal class BalanceSheetControllerTest : SpringBootBaseTest() {
    @Autowired
    @Qualifier("conversionService")
    private lateinit var conversionService: ConversionService
    @Autowired
    private lateinit var balanceSheetController: BalanceSheetController

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

        val actual = balanceSheetController.getBalanceSheetInfo(interval).data

        val dayStatM1 = BsDayStatDto(localDateToLong(m1Date), BigDecimal("100100.00"), BigDecimal("100100.00"),
            BigDecimal("100.00"))
        val dayStats = listOf(dayStatM1)

        val expected = BsStatDto(currentDate.minusDays(interval), currentDate,
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

        val expected = BsStatDto(currentDate.minusDays(interval), currentDate,
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

        val actual = balanceSheetController.getBalanceSheetInfo(interval).data

        val dayStatM1 = BsDayStatDto(localDateToLong(m1Date),
            totalSaldo = BigDecimal("99900.00"),
            freeAmount = BigDecimal("99900.00"),
            chargeAmount = BigDecimal("100.00"))
        val dayStatP1 = BsDayStatDto(localDateToLong(p1Date),
            totalSaldo = BigDecimal("99800.00"),
            freeAmount = BigDecimal("99800.00"),
            chargeAmount = BigDecimal("100.00"))
        val dayStats = listOf(dayStatM1, dayStatP1)

        val categories = run {
            val foodstuffsCategoryStatDto = requireNotNull(conversionService.convert(CategoryStat.of(foodstuffsTag,
                BigDecimal("100.00")), CategoryStatDto::class.java))
            listOf(foodstuffsCategoryStatDto)
        }
        val expected = BsStatDto(currentDate.minusDays(interval), currentDate,
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

}