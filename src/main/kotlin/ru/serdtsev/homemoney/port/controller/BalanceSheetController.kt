package ru.serdtsev.homemoney.port.controller

import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.port.service.StatService
import ru.serdtsev.homemoney.port.common.HmResponse
import ru.serdtsev.homemoney.port.common.HmResponse.Companion.getOk

@RestController
class BalanceSheetController(private val statService: StatService) {
    @RequestMapping("/api/bs-stat")
    @Transactional(readOnly = true)
    fun getBalanceSheetInfo(@RequestParam(defaultValue = "30") interval: Long): HmResponse {
        return getOk(statService.getBsStat(interval))
    }
}
