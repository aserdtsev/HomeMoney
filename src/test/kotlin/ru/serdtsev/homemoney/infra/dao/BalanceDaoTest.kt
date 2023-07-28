package ru.serdtsev.homemoney.infra.dao

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.account.Credit
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import java.math.BigDecimal

internal class BalanceDaoTest: SpringBootBaseTest() {
    @Autowired
    lateinit var balanceDao: BalanceDao

    @Test
    internal fun crud() {
        val balanceSheet = BalanceSheet()
        balanceSheetDao.save(balanceSheet)
        ApiRequestContextHolder.bsId = balanceSheet.id

        val balance = Balance(AccountType.debit, "name")
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
        val balance1 = Balance(AccountType.debit, "name")
        balanceDao.save(balance1)
        val balance2 = Balance(AccountType.debit, "name")
        balanceDao.save(balance2)

        balanceDao.findByBalanceSheet(balanceSheet).also {
            assertEquals(setOf(balance1, balance2), it.toSet())
        }
    }
}