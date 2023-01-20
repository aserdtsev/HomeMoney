package ru.serdtsev.homemoney.moneyoper.dao

import ru.serdtsev.homemoney.utils.TestHelper
import com.opentable.db.postgres.junit5.PreparedDbExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import ru.serdtsev.homemoney.account.dao.BalanceDao
import ru.serdtsev.homemoney.account.dao.ReserveDao
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.BalanceSheetDao
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import java.math.BigDecimal
import java.time.LocalDate

internal class MoneyOperItemDaoTest {
    companion object {
        @JvmField @RegisterExtension
        val db: PreparedDbExtension = TestHelper.db
    }

    lateinit var balanceSheetDao: BalanceSheetDao
    lateinit var balanceDao: BalanceDao
    lateinit var reserveDao: ReserveDao
    lateinit var tagDao: TagDao
    lateinit var moneyOperItemDao: MoneyOperItemDao
    lateinit var moneyOperDao: MoneyOperDao
    lateinit var balanceSheet: BalanceSheet
    lateinit var moneyOper: MoneyOper
    lateinit var balanceA: Balance
    lateinit var balanceB: Balance
    lateinit var value: BigDecimal

    @BeforeEach
    internal fun setUp() {
        val jdbcTemplate = NamedParameterJdbcTemplate(db.testDatabase)
        balanceSheetDao = BalanceSheetDao(jdbcTemplate)
        reserveDao = ReserveDao(jdbcTemplate, balanceSheetDao)
        balanceDao = BalanceDao(jdbcTemplate, balanceSheetDao, reserveDao)
        tagDao = TagDao(jdbcTemplate, balanceSheetDao)
        moneyOperItemDao = MoneyOperItemDao(jdbcTemplate, balanceDao)
        moneyOperDao = MoneyOperDao(jdbcTemplate, balanceSheetDao, moneyOperItemDao, tagDao)

        balanceSheet = createBalanceSheet()
        moneyOper = MoneyOper(balanceSheet, MoneyOperStatus.done).apply { moneyOperDao.save(this) }
        balanceA = Balance(balanceSheet, AccountType.debit, "a").apply { balanceDao.save(this) }
        balanceB = Balance(balanceSheet, AccountType.debit, "b").apply { balanceDao.save(this) }
    }

    @Test
    internal fun crud() {
        val value = BigDecimal.ONE.setScale(2)
        val item1 = moneyOper.addItem(balanceA, value.negate()).apply { moneyOperItemDao.save(this) }
        val item2 =moneyOper.addItem(balanceB, value).apply { moneyOperItemDao.save(this) }

        assertTrue(moneyOperItemDao.exists(item1.id))
        assertEquals(item1, moneyOperItemDao.findById(item1.id))
        assertEquals(listOf(item1, item2), moneyOperItemDao.findByMoneyOperId(moneyOper.id))
        assertEquals(listOf(item1), moneyOperItemDao.findByBalance(balanceA))

        with(item1) {
            this.index = 1
            this.value = value + BigDecimal.TEN
            this.balance = balanceB
        }
        moneyOperItemDao.save(item1)

        val actual = moneyOperItemDao.findById(item1.id)
        assertEquals(item1, actual)

        moneyOperItemDao.deleteByMoneyOperId(moneyOper.id);
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