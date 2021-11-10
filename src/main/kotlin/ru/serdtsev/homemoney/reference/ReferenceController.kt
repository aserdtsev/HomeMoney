package ru.serdtsev.homemoney.reference

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.common.HmResponse.Companion.getOk
import ru.serdtsev.homemoney.reference.service.ReferenceService

@RestController
@RequestMapping("/api/references")
class ReferenceController (private val referencesService: ReferenceService) {
    @GetMapping("currencies")
    fun getCurrencies(): HmResponse {
        val currencies = referencesService.getCurrencies()
        return getOk(currencies)
    }
}