package ru.serdtsev.homemoney.port.dao

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.domain.model.account.Reserve
import java.math.BigDecimal

internal class ReserveDaoTest: SpringBootBaseTest() {
    @Autowired
    private lateinit var reserveDao: ReserveDao

    @Test
    internal fun crud() {
        val reserve = Reserve("name")
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
        val reserve1 = Reserve("name")
        reserveDao.save(reserve1)
        val reserve2 = Reserve("name")
        reserveDao.save(reserve2)

        reserveDao.findByBalanceSheet(balanceSheet).also {
            assertEquals(setOf(reserve1, reserve2), it.toSet())
        }
    }
}