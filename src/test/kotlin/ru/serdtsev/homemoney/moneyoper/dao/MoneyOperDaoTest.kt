package ru.serdtsev.homemoney.moneyoper.dao

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.account.dao.BalanceDao
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import ru.serdtsev.homemoney.moneyoper.model.Tag
import java.math.BigDecimal

internal class MoneyOperDaoTest: SpringBootBaseTest() {
    @Autowired
    lateinit var balanceDao: BalanceDao
    @Autowired
    lateinit var moneyOperDao: MoneyOperDao

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