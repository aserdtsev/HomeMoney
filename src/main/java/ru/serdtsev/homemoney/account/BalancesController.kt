package ru.serdtsev.homemoney.account

import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.account.BalanceService
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.common.HmException
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.common.HmResponse.Companion.getFail
import ru.serdtsev.homemoney.common.HmResponse.Companion.getOk
import java.util.*

@RestController
@RequestMapping("/api/{bsId}/balances")
class BalancesController(private val balanceService: BalanceService) {
    @RequestMapping
    @Transactional(readOnly = true)
    fun getBalances(@PathVariable bsId: UUID): HmResponse {
        val balances = balanceService.getBalances(bsId)
        return getOk(balances)
    }

    @RequestMapping("/create")
    @Transactional
    fun createBalance(
            @PathVariable bsId: UUID,
            @RequestBody balance: Balance): HmResponse {
        return try {
            balanceService.createBalance(bsId, balance)
            getOk()
        } catch (e: HmException) {
            getFail(e.code.name)
        }
    }

    @RequestMapping("/update")
    @Transactional
    fun updateBalance(
            @PathVariable bsId: UUID,
            @RequestBody balance: Balance
    ): HmResponse {
        return try {
            balanceService.updateBalance(balance)
            getOk()
        } catch (e: HmException) {
            getFail(e.code.name)
        }
    }

    @RequestMapping("/delete")
    @Transactional
    fun deleteOrArchiveBalance(
            @PathVariable bsId: UUID,
            @RequestBody balance: Balance
    ): HmResponse {
        return try {
            balanceService.deleteOrArchiveBalance(balance.id)
            getOk()
        } catch (e: HmException) {
            getFail(e.code.name)
        }
    }

    @RequestMapping("/up")
    @Transactional
    fun upBalance(
            @PathVariable bsId: UUID,
            @RequestBody balance: Balance
    ): HmResponse {
        return try {
            balanceService.upBalance(bsId, balance.id)
            getOk()
        } catch (e: HmException) {
            getFail(e.code.name)
        }
    }
}