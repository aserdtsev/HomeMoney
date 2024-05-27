package ru.serdtsev.homemoney.port.dao

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.AnnuityPayment
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.account.Credit
import ru.serdtsev.homemoney.domain.model.moneyoper.DayRecurrenceParams
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import ru.serdtsev.homemoney.domain.model.moneyoper.MonthRecurrenceParams
import ru.serdtsev.homemoney.domain.model.moneyoper.Period
import ru.serdtsev.homemoney.domain.model.moneyoper.RecurrenceParams
import ru.serdtsev.homemoney.domain.model.moneyoper.Tag
import ru.serdtsev.homemoney.domain.model.moneyoper.WeekRecurrenceParams
import ru.serdtsev.homemoney.domain.model.moneyoper.YearRecurrenceParams
import java.math.BigDecimal
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate

internal class MoneyOperDaoTest: SpringBootBaseTest() {
    @Autowired
    lateinit var balanceDao: BalanceDao
    @Autowired
    lateinit var moneyOperDao: MoneyOperDao

    lateinit var balanceA: Balance
    lateinit var balanceB: Balance
    lateinit var tagA: Tag
    lateinit var tagB: Tag

    @BeforeEach
    internal fun setUp() {
        balanceA = run {
            val annuityPayment = AnnuityPayment(BigDecimal("1000.00"))
            val credit = Credit(BigDecimal("400000.00"), 12, 6, annuityPayment = annuityPayment)
            Balance(AccountType.debit, "a", credit = credit).apply { balanceDao.save(this) }
        }
        balanceB = Balance(AccountType.debit, "b").apply { balanceDao.save(this) }
        tagA = Tag.of("tagA")
        tagB = Tag.of("tagB")
    }

    @Test
    internal fun crud() {
        val value = BigDecimal("-1.00")
        val moneyOper = MoneyOper(MoneyOperStatus.Done, tags = listOf(tagA), comment = "comment")
            .apply {
                addItem(balanceA, value)
                moneyOperDao.save(this)
            }

        assertThat(moneyOperDao.findById(moneyOper.id))
            .usingRecursiveAssertion()
            .isEqualTo(moneyOper)

        with (moneyOper) {
            comment = "new comment"
            items[0].repaymentSchedule!![0].endDate = LocalDate.now()
        }
        moneyOperDao.save(moneyOper)

        assertThat(moneyOperDao.findById(moneyOper.id))
            .usingRecursiveAssertion()
            .isEqualTo(moneyOper)

    }

    @ParameterizedTest
    @MethodSource("params for save new MoneyOper by recurrenceParams")
    internal fun `save new MoneyOper by recurrenceParams`(period: Period, recurrenceParams: RecurrenceParams) {
        val moneyOper = MoneyOper(MoneyOperStatus.Done, period = period, recurrenceParams = recurrenceParams)
            .apply {
                addItem(balanceA, BigDecimal("-1.00"))
                moneyOperDao.save(this)
            }

        val actual = moneyOperDao.findById(moneyOper.id).recurrenceParams

        assertEquals(recurrenceParams, actual)
    }

    @Test
    internal fun findByBalanceSheetAndValueOrderByPerformedDesc() {
        val value = BigDecimal("1.00")
        val moneyOper1 = MoneyOper(MoneyOperStatus.Done, dateNum = 0).apply {
            addItem(balanceA, value.negate())
            addItem(balanceB, value)
            moneyOperDao.save(this)
        }
        val moneyOper2 = MoneyOper(MoneyOperStatus.Done, dateNum = 1).apply {
            addItem(balanceB, value.negate())
            addItem(balanceA, value)
            moneyOperDao.save(this)
        }
        MoneyOper(MoneyOperStatus.Done, dateNum = 2).apply {
            addItem(balanceA, BigDecimal("-2.00"))
            moneyOperDao.save(this)
        }

        with(PageRequest.of(0, 1)) {
            val expected = PageImpl(listOf(moneyOper2), this, 2)
            val actual =
                moneyOperDao.findByValueOrderByPerformedDesc(value, this)
            assertEquals(expected.totalPages, actual.totalPages)
            assertEquals(expected.content, actual.content)
        }
        with(PageRequest.of(1, 1)) {
            val expected = PageImpl(listOf(moneyOper1), this, 2)
            val actual =
                moneyOperDao.findByValueOrderByPerformedDesc(value, this)
            assertEquals(expected.totalPages, actual.totalPages)
            assertEquals(expected.content, actual.content)
        }
        with(PageRequest.of(0, 2)) {
            val expected = PageImpl(listOf(moneyOper2, moneyOper1), this, 1)
            val actual =
                moneyOperDao.findByValueOrderByPerformedDesc(value, this)
            assertEquals(expected.totalPages, actual.totalPages)
            assertEquals(expected.content, actual.content)
        }
    }

    @Test
    internal fun findByBalanceSheetAndPerformedBetweenAndMoneyOperStatus() {
        val moneyOper = MoneyOper(MoneyOperStatus.Done)
        moneyOperDao.save(moneyOper)

        moneyOperDao.findByPerformedBetweenAndMoneyOperStatus(LocalDate.now(), LocalDate.now(),
            MoneyOperStatus.Done).also { assertEquals(listOf(moneyOper), it) }
    }

    @Test
    internal fun findTrend() {
    }

    companion object {
        @JvmStatic
        private fun `params for save new MoneyOper by recurrenceParams`(): List<Arguments> =
            listOf(
                arguments(Period.Day, DayRecurrenceParams(1)),
                arguments(Period.Week, WeekRecurrenceParams(listOf(MONDAY, SUNDAY))),
                arguments(Period.Month, MonthRecurrenceParams(1)),
                arguments(Period.Year, YearRecurrenceParams(1, 1))
            )
    }
}