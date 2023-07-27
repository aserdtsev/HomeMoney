package ru.serdtsev.homemoney.port.controller

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.port.dto.account.BalanceDto
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.port.service.BalanceService
import ru.serdtsev.homemoney.port.common.HmResponse
import ru.serdtsev.homemoney.port.common.HmResponse.Companion.getOk

@RestController
@RequestMapping("/api/balances")
class BalanceController(
    private val balanceService: BalanceService,
    @Qualifier("conversionService") private val conversionService: ConversionService
) {
    @RequestMapping
    fun getBalances(): HmResponse {
        val balances = balanceService.getBalances()
            .sortedBy { it.num }
            .map { conversionService.convert(it, BalanceDto::class.java)!! }
        return getOk(balances)
    }

    @RequestMapping("/create")
    fun createBalance(@RequestBody balanceDto: BalanceDto): HmResponse {
        val balance = conversionService.convert(balanceDto, Balance::class.java) as Balance
        balanceService.createBalance(balance)
        return getOk()
    }

    @RequestMapping("/update")
    fun updateBalance(@RequestBody balanceDto: BalanceDto): HmResponse {
        val balance = conversionService.convert(balanceDto, Balance::class.java) as Balance
        balanceService.updateBalance(balance)
        return getOk()
    }

    @RequestMapping("/delete")
    fun deleteOrArchiveBalance(@RequestBody balanceDto: BalanceDto): HmResponse {
        balanceService.deleteOrArchiveBalance(balanceDto.id)
        return getOk()
    }

    @RequestMapping("/up")
    fun upBalance(@RequestBody balanceDto: BalanceDto): HmResponse {
        balanceService.upBalance(balanceDto.id)
        return getOk()
    }
}