package ru.serdtsev.homemoney.moneyoper.dao

import ru.serdtsev.homemoney.utils.TestHelper
import com.opentable.db.postgres.junit5.PreparedDbExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
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
import ru.serdtsev.homemoney.moneyoper.model.Tag
import java.math.BigDecimal

internal class MoneyOperDaoTest {
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

    @BeforeEach
    internal fun setUp() {
        val jdbcTemplate = NamedParameterJdbcTemplate(db.testDatabase)
        balanceSheetDao = BalanceSheetDao(jdbcTemplate)
        reserveDao = ReserveDao(jdbcTemplate, balanceSheetDao)
        balanceDao = BalanceDao(jdbcTemplate, balanceSheetDao, reserveDao)
        tagDao = TagDao(jdbcTemplate, balanceSheetDao)
        moneyOperItemDao = MoneyOperItemDao(jdbcTemplate, balanceDao)
        moneyOperDao = MoneyOperDao(jdbcTemplate, balanceSheetDao, moneyOperItemDao, tagDao)
    }

    @Test
    internal fun crud() {
        val balanceSheet = createBalanceSheet()
        val tags = listOf(Tag(balanceSheet, "tag1"), Tag(balanceSheet, "tag2"))
        val moneyOper = MoneyOper(balanceSheet, MoneyOperStatus.done, comment = "comment", tags = tags)

        val balance = Balance(balanceSheet, AccountType.debit, "a")
        balanceDao.save(balance)

        val value = BigDecimal.ONE.setScale(2)
        moneyOper.addItem(balance, value)
        moneyOperDao.save(moneyOper)

        val actual = moneyOperDao.findById(moneyOper.id)

        assertEquals(moneyOper, actual)
    }

    private fun createBalanceSheet(): BalanceSheet = BalanceSheet().apply {
        balanceSheetDao.save(this)
        ApiRequestContextHolder.bsId = id
    }
}