package ru.serdtsev.homemoney.moneyoper.dao

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.account.dao.BalanceDao
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import java.math.BigDecimal
import java.time.LocalDate

internal class MoneyOperItemDaoTest: SpringBootBaseTest() {
    @Autowired
    lateinit var balanceDao: BalanceDao
    @Autowired
    lateinit var moneyOperItemDao: MoneyOperItemDao
    @Autowired
    lateinit var moneyOperDao: MoneyOperDao

    lateinit var balanceSheet: BalanceSheet
    lateinit var moneyOper: MoneyOper
    lateinit var balanceA: Balance
    lateinit var balanceB: Balance
    lateinit var value: BigDecimal

    @BeforeEach
    internal fun setUp() {
        balanceSheet = createBalanceSheet()
        moneyOper = MoneyOper(balanceSheet, MoneyOperStatus.done).apply { moneyOperDao.save(this) }
        balanceA = Balance(balanceSheet, AccountType.debit, "a").apply { balanceDao.save(this) }
        balanceB = Balance(balanceSheet, AccountType.debit, "b").apply { balanceDao.save(this) }
    }

    @Test
    internal fun crud() {
        val value = BigDecimal("1.00")
        val item1 = moneyOper.addItem(balanceA, value.negate())
            .apply { moneyOperItemDao.save(this) }
        val item2 = moneyOper.addItem(balanceB, value)
            .apply { moneyOperItemDao.save(this) }

        assertTrue(moneyOperItemDao.exists(item1.id))
        assertEquals(item1, moneyOperItemDao.findById(item1.id))
        assertEquals(listOf(item1, item2), moneyOperItemDao.findByMoneyOperId(moneyOper.id))
        assertEquals(listOf(item1), moneyOperItemDao.findByBalance(balanceA))

        with (item1) {
            this.index = 1
            this.value += BigDecimal("10.00")
            this.balance = balanceB
        }
        balanceB.value += BigDecimal("1.00")
        moneyOperItemDao.save(item1)

        val actual = moneyOperItemDao.findById(item1.id)
        assertThat(actual).isEqualTo(item1)
        assertThat(actual.balance).isEqualTo(balanceB)

        moneyOperItemDao.deleteByMoneyOperId(moneyOper.id)
        assertTrue(moneyOperItemDao.findByMoneyOperId(moneyOper.id).isEmpty())
    }

    @Test
    internal fun findByBalanceSheetAndPerformedBetweenAndMoneyOperStatus() {
        val value = BigDecimal.ONE.setScale(2)
        val item1 = moneyOper.addItem(balanceA, value.negate()).apply { moneyOperItemDao.save(this) }
        val item2 =moneyOper.addItem(balanceB, value).apply { moneyOperItemDao.save(this) }

        moneyOperItemDao.findByBalanceSheetAndPerformedBetweenAndMoneyOperStatus(balanceSheet, LocalDate.now(),
            LocalDate.now(), MoneyOperStatus.done).also { assertEquals(listOf(item1, item2), it) }
    }

    @Test
    internal fun findByBalanceSheetAndValueOrderByPerformedDesc() {
        val value = BigDecimal.ONE.setScale(2)
        val item1 = moneyOper.addItem(balanceA, value.negate()).apply { moneyOperItemDao.save(this) }
        val item2 =moneyOper.addItem(balanceB, value).apply { moneyOperItemDao.save(this) }

        with(PageRequest.of(0, 1)) {
            assertEquals(PageImpl(listOf(item1), this, 2),
                moneyOperItemDao.findByBalanceSheetAndValueOrderByPerformedDesc(balanceSheet, value,this)
            )
        }
        with(PageRequest.of(1, 1)) {
            assertEquals(PageImpl(listOf(item2), this, 2),
                moneyOperItemDao.findByBalanceSheetAndValueOrderByPerformedDesc(balanceSheet, value,this)
            )
        }
        with(PageRequest.of(0, 2)) {
            assertEquals(PageImpl(listOf(item1, item2), this, 1),
                moneyOperItemDao.findByBalanceSheetAndValueOrderByPerformedDesc(balanceSheet, value,this)
            )
        }
    }

    private fun createBalanceSheet(): BalanceSheet = BalanceSheet().apply {
        balanceSheetDao.save(this)
        ApiRequestContextHolder.bsId = id
    }
}