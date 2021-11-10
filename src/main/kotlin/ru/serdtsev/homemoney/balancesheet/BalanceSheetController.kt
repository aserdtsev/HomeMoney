package ru.serdtsev.homemoney.balancesheet

import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.balancesheet.service.StatService
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.common.HmResponse.Companion.getOk

@RestController
class BalanceSheetController(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val statService: StatService
) {
    @RequestMapping("/api/bs-stat")
    @Transactional(readOnly = true)
    fun getBalanceSheetInfo(@RequestParam(defaultValue = "30") interval: Long): HmResponse {
        return getOk(statService.getBsStat(apiRequestContextHolder.getBsId(), interval))
    }
}
