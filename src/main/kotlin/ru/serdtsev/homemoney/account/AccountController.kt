package ru.serdtsev.homemoney.account

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.account.dto.AccountDto
import ru.serdtsev.homemoney.account.service.AccountService
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.common.HmResponse.Companion.getOk

@RestController
class AccountController(
    private val accountService: AccountService,
    @Qualifier("conversionService") private val conversionService: ConversionService
) {
    @RequestMapping("/api/accounts")
    @Transactional(readOnly = true)
    fun getAccounts(): HmResponse {
        val list = accountService.getAccounts()
            .sortedBy { it.getSortIndex() }
            .map { conversionService.convert(it, AccountDto::class.java) }
        return getOk(list)
    }
}