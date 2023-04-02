package ru.serdtsev.homemoney.account.dao

import com.opentable.db.postgres.junit5.PreparedDbExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.account.model.Credit
import ru.serdtsev.homemoney.balancesheet.BalanceSheetDao
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.utils.TestHelper
import java.math.BigDecimal
import java.util.*

internal class BalanceDaoTest {
    companion object {
        @JvmField @RegisterExtension
        val db: PreparedDbExtension = TestHelper.db
    }

    lateinit var balanceSheetDao: BalanceSheetDao
    lateinit var reserveDao: ReserveDao
    lateinit var balanceDao: BalanceDao

    @BeforeEach
    internal fun setUp() {
        val jdbcTemplate = NamedParameterJdbcTemplate(db.testDatabase)
        balanceSheetDao = BalanceSheetDao(jdbcTemplate)
        ApiRequestContextHolder.requestId = UUID.randomUUID().toString()
        reserveDao = ReserveDao(jdbcTemplate, balanceSheetDao)
        balanceDao = BalanceDao(jdbcTemplate, balanceSheetDao, reserveDao)
    }

    @Test
    internal fun crud() {
        val balanceSheet = BalanceSheet()
        balanceSheetDao.save(balanceSheet)
        ApiRequestContextHolder.bsId = balanceSheet.id

        val balance = Balance(balanceSheet, AccountType.debit, "name")
        balanceDao.save(balance)

        assertTrue(balanceDao.exists(balance.id))

        balanceDao.findByIdOrNull(balance.id).also { actual ->
            assertNotNull(actual)
            assertEquals(balance, actual)
        }

        balanceDao.findById(balance.id).also { actual ->
            assertEquals(balance, actual)
        }

        balance.value = BigDecimal.ONE
        balance.minValue = balance.minValue.plus(BigDecimal.ONE)
        balance.credit = Credit(BigDecimal.ONE)
        balance.num = 1
        balance.isArc = true

        balanceDao.save(balance)
        balanceDao.findByIdOrNull(balance.id).also { actual ->
            assertNotNull(actual)
            assertEquals(actual, balance)
        }

        balanceDao.delete(balance)
        assertFalse(balanceDao.exists(balance.id))
    }

    @Test
    internal fun findByBalanceSheet() {
        val balanceSheetA = BalanceSheet()
        balanceSheetDao.save(balanceSheetA)
        val balanceA1 = Balance(balanceSheetA, AccountType.debit, "name")
        balanceDao.save(balanceA1)
        val balanceA2 = Balance(balanceSheetA, AccountType.debit, "name")
        balanceDao.save(balanceA2)

        val balanceSheetB = BalanceSheet()
        balanceSheetDao.save(balanceSheetB)
        val balanceB1 = Balance(balanceSheetB, AccountType.debit, "name")
        balanceDao.save(balanceB1)

        balanceDao.findByBalanceSheet(balanceSheetA).also {
            assertEquals(setOf(balanceA1, balanceA2), it.toSet())
        }

        balanceDao.findByBalanceSheet(balanceSheetB).also {
            assertEquals(listOf(balanceB1), it)
        }
    }
}