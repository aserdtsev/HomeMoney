package ru.serdtsev.homemoney.account

import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.common.HmException
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.common.HmResponse.Companion.getFail
import ru.serdtsev.homemoney.common.HmResponse.Companion.getOk
import java.util.*

@RestController
@RequestMapping("/api/balances")
class BalancesController(private val apiRequestContextHolder: ApiRequestContextHolder,
        private val balanceService: BalanceService) {
    @RequestMapping
    @Transactional(readOnly = true)
    fun getBalances(): HmResponse {
        val balances = balanceService.getBalances(apiRequestContextHolder.getBsId())
        return getOk(balances)
    }

    @RequestMapping("/create")
    @Transactional
    fun createBalance(@RequestBody balance: Balance): HmResponse {
        return try {
            balanceService.createBalance(apiRequestContextHolder.getBsId(), balance)
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