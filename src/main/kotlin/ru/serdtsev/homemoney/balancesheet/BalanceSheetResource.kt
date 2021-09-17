package ru.serdtsev.homemoney.balancesheet

import mu.KotlinLogging
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.common.HmException
import ru.serdtsev.homemoney.common.HmResponse

@RestController
class BalanceSheetResource(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val statService: StatService) {
    private val log = KotlinLogging.logger {  }

    @RequestMapping("/api/bs-stat")
    @Transactional(readOnly = true)
    fun getBalanceSheetInfo(@RequestParam(defaultValue = "30") interval: Long): HmResponse {
        log.info {"bs-stat start" }
        val response = try {
            HmResponse.getOk(statService.getBsStat(apiRequestContextHolder.getBsId(), interval))
        } catch (e: HmException) {
            HmResponse.getFail("INCORRECT_AUTH_TOKEN")
        }
        log.info { "bs-stat end" }
        return response
    }
}
