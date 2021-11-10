package ru.serdtsev.homemoney.account.service

import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.account.model.Account
import ru.serdtsev.homemoney.common.ApiRequestContextHolder

@Service
class AccountService(private val apiRequestContextHolder: ApiRequestContextHolder) {
    fun getAccounts(): List<Account> = apiRequestContextHolder.getBalanceSheet().accounts
}