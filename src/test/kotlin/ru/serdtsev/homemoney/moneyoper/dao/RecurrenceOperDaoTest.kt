package ru.serdtsev.homemoney.moneyoper.dao

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import ru.serdtsev.homemoney.moneyoper.model.RecurrenceOper
import java.time.LocalDate

internal class RecurrenceOperDaoTest: SpringBootBaseTest() {
    @Autowired
    lateinit var moneyOperDao: MoneyOperDao
    @Autowired
    lateinit var recurrenceOperDao: RecurrenceOperDao

    lateinit var balanceSheet: BalanceSheet
    lateinit var recurrenceOper: RecurrenceOper

    @BeforeEach
    internal fun setUp() {
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