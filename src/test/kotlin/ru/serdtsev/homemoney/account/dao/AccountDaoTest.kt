package ru.serdtsev.homemoney.account.dao

import ru.serdtsev.homemoney.utils.TestHelper
import com.opentable.db.postgres.junit5.PreparedDbExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.BalanceSheetDao
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import java.util.*

internal class AccountDaoTest {
    companion object {
        @JvmField @RegisterExtension
        val db: PreparedDbExtension = TestHelper.db
    }

    lateinit var balanceSheetDao: BalanceSheetDao
    lateinit var reserveDao: ReserveDao
    lateinit var balanceDao: BalanceDao
    lateinit var accountDao: AccountDao

    @BeforeEach
    internal fun setUp() {
        val jdbcTemplate = NamedParameterJdbcTemplate(db.testDatabase)
        balanceSheetDao = BalanceSheetDao(jdbcTemplate)
        reserveDao = ReserveDao(jdbcTemplate, balanceSheetDao)
        balanceDao = BalanceDao(jdbcTemplate, balanceSheetDao, reserveDao)
        accountDao = AccountDao(jdbcTemplate)
    }

    @Test
    internal fun findNameByIdOrNull() {
        val balanceSheet = BalanceSheet()
        balanceSheetDao.save(balanceSheet)
        ApiRequestContextHolder.bsId = balanceSheet.id

        val balance = Balance(balanceSheet, AccountType.debit, "name")
        balanceDao.save(balance)

        accountDao.findNameByIdOrNull(balance.id).also { assertEquals(balance.name, it) }

        assertNull(accountDao.findNameByIdOrNull(UUID.randomUUID()))

        accountDao.findNameById(balance.id).also { assertEquals(balance.name, it) }
    }
}