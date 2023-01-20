package ru.serdtsev.homemoney.moneyoper.dao

import ru.serdtsev.homemoney.utils.TestHelper
import com.opentable.db.postgres.junit5.PreparedDbExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import ru.serdtsev.homemoney.account.dao.BalanceDao
import ru.serdtsev.homemoney.account.dao.ReserveDao
import ru.serdtsev.homemoney.balancesheet.BalanceSheetDao
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import ru.serdtsev.homemoney.moneyoper.model.RecurrenceOper
import java.time.LocalDate

internal class RecurrenceOperDaoTest {
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
    lateinit var recurrenceOperDao: RecurrenceOperDao
    lateinit var balanceSheet: BalanceSheet
    lateinit var recurrenceOper: RecurrenceOper

    @BeforeEach
    internal fun setUp() {
        val jdbcTemplate = NamedParameterJdbcTemplate(db.testDatabase)
        balanceSheetDao = BalanceSheetDao(jdbcTemplate)
        reserveDao = ReserveDao(jdbcTemplate, balanceSheetDao)
        balanceDao = BalanceDao(jdbcTemplate, balanceSheetDao, reserveDao)
        tagDao = TagDao(jdbcTemplate, balanceSheetDao)
        moneyOperItemDao = MoneyOperItemDao(jdbcTemplate, balanceDao)
        moneyOperDao = MoneyOperDao(jdbcTemplate, balanceSheetDao, moneyOperItemDao, tagDao)
        recurrenceOperDao = RecurrenceOperDao(jdbcTemplate, balanceSheetDao, moneyOperDao)
        balanceSheet = createBalanceSheet()
        recurrenceOper = createRecurrenceOper(balanceSheet)
        recurrenceOperDao.save(recurrenceOper)
    }

    @Test
    fun findById() {
        assertEquals(recurrenceOper, recurrenceOperDao.findById(recurrenceOper.id))
    }

    @Test
    fun findByIdOrNull() {
        assertEquals(recurrenceOper, recurrenceOperDao.findByIdOrNull(recurrenceOper.id))
    }

    @Test
    fun exists() {
        assertTrue(recurrenceOperDao.exists(recurrenceOper.id))
    }

    @Test
    fun findByBalanceSheetAndArc() {
        assertEquals(listOf(recurrenceOper), recurrenceOperDao.findByBalanceSheetAndArc(balanceSheet))
        assertEquals(listOf(recurrenceOper), recurrenceOperDao.findByBalanceSheetAndArc(balanceSheet, false))
        assertTrue(recurrenceOperDao.findByBalanceSheetAndArc(balanceSheet, true).isEmpty())

        recurrenceOper.arc = true
        recurrenceOperDao.save(recurrenceOper)
        assertEquals(listOf(recurrenceOper), recurrenceOperDao.findByBalanceSheetAndArc(balanceSheet, true))
    }

    private fun createBalanceSheet(): BalanceSheet = BalanceSheet().apply {
        balanceSheetDao.save(this)
        ApiRequestContextHolder.bsId = id
    }

    private fun createRecurrenceOper(balanceSheet: BalanceSheet): RecurrenceOper {
        val template = MoneyOper(balanceSheet, MoneyOperStatus.done)
        moneyOperDao.save(template)
        return RecurrenceOper(balanceSheet, template, LocalDate.now().plusDays(1))
    }
}