package ru.serdtsev.homemoney.account

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.account.dto.BalanceDto
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.common.HmException
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.common.HmResponse.Companion.getFail
import ru.serdtsev.homemoney.common.HmResponse.Companion.getOk

@RestController
@RequestMapping("/api/balances")
class BalanceController(
    private val balanceService: BalanceService,
    @Qualifier("conversionService") private val conversionService: ConversionService
) {
    @RequestMapping
    @Transactional(readOnly = true)
    fun getBalances(): HmResponse {
        val balances = balanceService.getBalances()
            .map { conversionService.convert(it, BalanceDto::class.java)!! }
        return getOk(balances)
    }

    @RequestMapping("/create")
    @Transactional
    fun createBalance(@RequestBody balanceDto: BalanceDto): HmResponse {
        return try {
            val balance = conversionService.convert(balanceDto, Balance::class.java) as Balance
            balanceService.createBalance(balance)
            getOk()
        } catch (e: HmException) {
            getFail(e.code.name)
        }
    }

    @RequestMapping("/update")
    @Transactional
    fun updateBalance(@RequestBody balanceDto: BalanceDto): HmResponse {
        return try {
            val balance = conversionService.convert(balanceDto, Balance::class.java) as Balance
            balanceService.updateBalance(balance)
            getOk()
        } catch (e: HmException) {
            getFail(e.code.name)
        }
    }

    @RequestMapping("/delete")
    @Transactional
    fun deleteOrArchiveBalance(@RequestBody balance: Balance): HmResponse {
        return try {
            balanceService.deleteOrArchiveBalance(balance.id)
            getOk()
        } catch (e: HmException) {
            getFail(e.code.name)
        }
    }

    @RequestMapping("/up")
    @Transactional
    fun upBalance(@RequestBody balance: Balance): HmResponse {
        return try {
            balanceService.upBalance(balance.id)
            getOk()
        } catch (e: HmException) {
            getFail(e.code.name)
        }
    }
}