package ru.serdtsev.homemoney.account.service

import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.account.dao.BalanceDao
import ru.serdtsev.homemoney.account.dao.ReserveDao
import ru.serdtsev.homemoney.account.model.Account
import ru.serdtsev.homemoney.common.ApiRequestContextHolder

@Service
class AccountService(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val balanceDao: BalanceDao,
    private val reserveDao: ReserveDao
) {
    fun getAccounts(): List<Account> {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        return balanceDao.findByBalanceSheet(balanceSheet).plus(reserveDao.findByBalanceSheet(balanceSheet))
    }
}