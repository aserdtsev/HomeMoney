package ru.serdtsev.homemoney.account.dao

import ru.serdtsev.homemoney.utils.TestHelper
import com.opentable.db.postgres.junit5.PreparedDbExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import ru.serdtsev.homemoney.account.model.Reserve
import ru.serdtsev.homemoney.balancesheet.BalanceSheetDao
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import java.math.BigDecimal

internal class ReserveDaoTest {
    companion object {
        @JvmField @RegisterExtension
        val db: PreparedDbExtension = TestHelper.db
    }

    private lateinit var balanceSheetDao: BalanceSheetDao
    private lateinit var reserveDao: ReserveDao

    @BeforeEach
    internal fun setUp() {
        val jdbcTemplate = NamedParameterJdbcTemplate(db.testDatabase)
        balanceSheetDao = BalanceSheetDao(jdbcTemplate)
        reserveDao = ReserveDao(jdbcTemplate, balanceSheetDao)
    }

    @Test
    internal fun crud() {
        val balanceSheet = BalanceSheet()
        balanceSheetDao.save(balanceSheet)
        ApiRequestContextHolder.bsId = balanceSheet.id

        val reserve = Reserve(balanceSheet, "name")
        reserveDao.save(reserve)

        assertTrue(reserveDao.exists(reserve.id))

        reserveDao.findByIdOrNull(reserve.id).also { actual ->
            assertNotNull(actual)
            assertEquals(reserve, actual)
        }

        reserveDao.findById(reserve.id).also { actual ->
            assertEquals(reserve, actual)
        }

        reserve.value = BigDecimal.ONE
        reserve.num = 1
        reserve.isArc = true

        reserveDao.save(reserve)
        reserveDao.findByIdOrNull(reserve.id).also { actual ->
            assertNotNull(actual)
            assertEquals(reserve, actual)
        }

        reserveDao.delete(reserve)
        assertFalse(reserveDao.exists(reserve.id))
    }

    @Test
    internal fun findByBalanceSheet() {
        val balanceSheetA = BalanceSheet()
        balanceSheetDao.save(balanceSheetA)
        val reserveA1 = Reserve(balanceSheetA, "name")
        reserveDao.save(reserveA1)
        val reserveA2 = Reserve(balanceSheetA, "name")
        reserveDao.save(reserveA2)

        val balanceSheetB = BalanceSheet()
        balanceSheetDao.save(balanceSheetB)
        val reserveB1 = Reserve(balanceSheetB, "name")
        reserveDao.save(reserveB1)

        reserveDao.findByBalanceSheet(balanceSheetA).also {
            assertEquals(setOf(reserveA1, reserveA2), it.toSet())
        }

        reserveDao.findByBalanceSheet(balanceSheetB).also {
            assertEquals(listOf(reserveB1), it)
        }
    }
}