package ru.serdtsev.homemoney.balancesheet

import ru.serdtsev.homemoney.utils.TestHelper
import com.opentable.db.postgres.junit5.PreparedDbExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet

internal class BalanceSheetDaoTest {
    companion object {
        @JvmField @RegisterExtension
        val db: PreparedDbExtension = TestHelper.db
    }

    private lateinit var dao: BalanceSheetDao

    @BeforeEach
    internal fun setUp() {
        val jdbcTemplate = NamedParameterJdbcTemplate(db.testDatabase)
        dao = BalanceSheetDao(jdbcTemplate)
    }

    @Test
    fun save() {
        val expected = BalanceSheet()
        val id = expected.id

        dao.save(expected)
        assertTrue(dao.exists(id))
        assertEquals(expected, dao.findByIdOrNull(id))

        dao.deleteById(id)
        assertNull(dao.findByIdOrNull(id))
    }

    @Test
    // todo Make
    fun getAggregateAccountSaldoList() {
    }
}