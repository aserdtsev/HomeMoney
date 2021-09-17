package ru.serdtsev.homemoney.account

import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.common.HmResponse.Companion.getOk

@RestController
class AccountsController(private val apiRequestContextHolder: ApiRequestContextHolder) {
    @RequestMapping("/api/accounts")
    @Transactional(readOnly = true)
    fun getAccountList(): HmResponse {
        val accounts = apiRequestContextHolder.getBalanceSheet().accounts?.sortedBy { it.getSortIndex() }
        return getOk(accounts)
    }
}