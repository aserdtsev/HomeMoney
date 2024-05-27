package ru.serdtsev.homemoney.port.dao

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.AnnuityPayment
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.account.Credit
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import java.math.BigDecimal

internal class BalanceDaoTest: SpringBootBaseTest() {
    @Autowired
    lateinit var balanceDao: BalanceDao

    @Test
    internal fun crud() {
        balanceSheetDao.save(balanceSheet)
        ApiRequestContextHolder.balanceSheet = balanceSheet

        val balance = run {
            val annuityPayment = AnnuityPayment(BigDecimal("1000.00"))
            val credit = Credit(BigDecimal("400000.00"), 12, 6, annuityPayment = annuityPayment)
            Balance(AccountType.debit, "name", credit = credit).apply { balanceDao.save(this) }
        }

        assertTrue(balanceDao.exists(balance.id))

        assertThat(balanceDao.findById(balance.id))
            .usingRecursiveComparison()
            .isEqualTo(balance)

        with (balance) {
            value = value.plus(BigDecimal.ONE)
            minValue = minValue.plus(BigDecimal.ONE)
            this.credit = Credit(BigDecimal("500000.00"), 13, 7,
                AnnuityPayment(BigDecimal("2000.00")))
            num = 1
            isArc = true
        }
        balanceDao.save(balance)
        assertThat(balanceDao.findById(balance.id))
            .usingRecursiveComparison()
            .isEqualTo(balance)

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