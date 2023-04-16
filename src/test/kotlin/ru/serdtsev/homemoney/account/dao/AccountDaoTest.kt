package ru.serdtsev.homemoney.account.dao

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.BalanceSheetDao
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import java.util.*

internal class AccountDaoTest: SpringBootBaseTest() {
    @Autowired
    lateinit var balanceDao: BalanceDao
    @Autowired
    lateinit var accountDao: AccountDao

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