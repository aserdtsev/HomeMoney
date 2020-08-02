package ru.serdtsev.homemoney.account

import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.common.HmResponse.Companion.getOk
import java.util.*

@RestController
class AccountsController (private val balanceSheetRepo: BalanceSheetRepository) {
    @RequestMapping("/api/{bsId}/accounts")
    @Transactional(readOnly = true)
    fun getAccountList(@PathVariable bsId: UUID): HmResponse {
        val balanceSheet = balanceSheetRepo.findByIdOrNull(bsId)!!
        val accounts = balanceSheet.accounts
                ?.sortedBy { it.getSortIndex() }
        return getOk(accounts)
    }
}