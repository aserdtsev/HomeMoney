package ru.serdtsev.homemoney.port.controller

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.port.common.HmResponse
import ru.serdtsev.homemoney.port.common.HmResponse.Companion.getOk
import ru.serdtsev.homemoney.port.dto.balancesheet.BsStatDto
import ru.serdtsev.homemoney.port.service.StatService
import java.time.LocalDate

@RestController
class BalanceSheetController(
    private val statService: StatService,
    @Qualifier("conversionService") private val conversionService: ConversionService
) {
    @RequestMapping("/api/bs-stat")
    fun getBalanceSheetInfo(@RequestParam(defaultValue = "30") interval: Long): HmResponse {
        val model = statService.getBsStat(LocalDate.now(), interval)
        val dto = conversionService.convert(model, BsStatDto::class.java)
        return getOk(dto)
    }
}
