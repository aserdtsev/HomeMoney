package ru.serdtsev.homemoney.infra.dao

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import ru.serdtsev.homemoney.domain.model.moneyoper.Tag
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import java.math.BigDecimal
import java.time.LocalDate

internal class MoneyOperDaoTest: SpringBootBaseTest() {
    @Autowired
    lateinit var balanceDao: BalanceDao
    @Autowired
    lateinit var tagDao: TagDao
    @Autowired
    lateinit var moneyOperDao: MoneyOperDao

    lateinit var balanceA: Balance
    lateinit var balanceB: Balance
    lateinit var tagA: Tag
    lateinit var tagB: Tag

    @BeforeEach
    internal fun setUp() {
        balanceA = Balance(balanceSheet, AccountType.debit, "a").apply { balanceDao.save(this) }
        balanceB = Balance(balanceSheet, AccountType.debit, "b").apply { balanceDao.save(this) }
        tagA = Tag(balanceSheet, "tagA").apply { tagDao.save(this) }
        tagB = Tag(balanceSheet, "tagB").apply { tagDao.save(this) }
    }

    @Test
    // todo Даписать тест
    internal fun crud() {
        val value = BigDecimal("1.00")
        val moneyOper = MoneyOper(balanceSheet, MoneyOperStatus.done, comment = "comment", tags = listOf(tagA))
            .apply {
                addItem(balanceA, value)
                moneyOperDao.save(this)
            }

        assertThat(moneyOperDao.findById(moneyOper.id))
            .usingRecursiveAssertion()
            .isEqualTo(moneyOper)

        with(moneyOper) {
            comment = "new comment"
        }
        moneyOperDao.save(moneyOper);

        assertThat(moneyOperDao.findById(moneyOper.id))
            .usingRecursiveAssertion()
            .isEqualTo(moneyOper)

    }

    @Test
    internal fun findByBalanceSheetAndValueOrderByPerformedDesc() {
        val value = BigDecimal("1.00")
        val moneyOper1 = MoneyOper(balanceSheet, MoneyOperStatus.done, dateNum = 0).apply {
            addItem(balanceA, value.negate())
            addItem(balanceB, value)
            moneyOperDao.save(this)
        }
        val moneyOper2 = MoneyOper(balanceSheet, MoneyOperStatus.done, dateNum = 1).apply {
            addItem(balanceB, value.negate())
            addItem(balanceA, value)
            moneyOperDao.save(this)
        }
        MoneyOper(balanceSheet, MoneyOperStatus.done, dateNum = 2).apply {
            addItem(balanceA, BigDecimal("-2.00"))
            moneyOperDao.save(this)
        }

        with(PageRequest.of(0, 1)) {
            val expected = PageImpl(listOf(moneyOper2), this, 2)
            val actual =
                moneyOperDao.findByBalanceSheetAndValueOrderByPerformedDesc(balanceSheet, value, this)
            assertEquals(expected.totalPages, actual.totalPages)
            assertEquals(expected.content, actual.content)
        }
        with(PageRequest.of(1, 1)) {
            val expected = PageImpl(listOf(moneyOper1), this, 2)
            val actual =
                moneyOperDao.findByBalanceSheetAndValueOrderByPerformedDesc(balanceSheet, value, this)
            assertEquals(expected.totalPages, actual.totalPages)
            assertEquals(expected.content, actual.content)
        }
        with(PageRequest.of(0, 2)) {
            val expected = PageImpl(listOf(moneyOper2, moneyOper1), this, 1)
            val actual =
                moneyOperDao.findByBalanceSheetAndValueOrderByPerformedDesc(balanceSheet, value, this)
            assertEquals(expected.totalPages, actual.totalPages)
            assertEquals(expected.content, actual.content)
        }
    }

    @Test
    internal fun findByBalanceSheetAndPerformedBetweenAndMoneyOperStatus() {
        val moneyOper = MoneyOper(balanceSheet, MoneyOperStatus.done)
        moneyOperDao.save(moneyOper)

        moneyOperDao.findByBalanceSheetAndPerformedBetweenAndMoneyOperStatus(balanceSheet, LocalDate.now(),
            LocalDate.now(), MoneyOperStatus.done).also { assertEquals(listOf(moneyOper), it) }
    }

    private fun createBalanceSheet(): BalanceSheet = BalanceSheet().apply {
        balanceSheetDao.save(this)
        ApiRequestContextHolder.bsId = id
    }
}