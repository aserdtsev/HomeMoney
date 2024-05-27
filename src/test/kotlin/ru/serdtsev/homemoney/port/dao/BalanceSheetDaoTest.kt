package ru.serdtsev.homemoney.port.dao

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet

internal class BalanceSheetDaoTest: SpringBootBaseTest() {
    @Autowired
    private lateinit var dao: BalanceSheetDao

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