package ru.serdtsev.homemoney.port.dao

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import ru.serdtsev.homemoney.domain.model.moneyoper.RecurrenceOper

internal class RecurrenceOperDaoTest: SpringBootBaseTest() {
    @Autowired
    lateinit var moneyOperDao: MoneyOperDao
    @Autowired
    lateinit var recurrenceOperDao: RecurrenceOperDao

    lateinit var recurrenceOper: RecurrenceOper

    @BeforeEach
    internal fun setUp() {
        recurrenceOper = createRecurrenceOper()
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
        assertEquals(listOf(recurrenceOper), recurrenceOperDao.findByBalanceSheetAndArc())
        assertEquals(listOf(recurrenceOper), recurrenceOperDao.findByBalanceSheetAndArc(false))
        assertTrue(recurrenceOperDao.findByBalanceSheetAndArc(true).isEmpty())

        recurrenceOper.arc = true
        recurrenceOperDao.save(recurrenceOper)
        assertEquals(listOf(recurrenceOper), recurrenceOperDao.findByBalanceSheetAndArc(true))
    }

    private fun createRecurrenceOper(): RecurrenceOper {
        val sample = MoneyOper(MoneyOperStatus.Done)
        moneyOperDao.save(sample)
        return RecurrenceOper.of(sample)
    }
}