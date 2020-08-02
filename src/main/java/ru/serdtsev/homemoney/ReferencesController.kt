package ru.serdtsev.homemoney

import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.common.HmResponse.Companion.getOk
import ru.serdtsev.homemoney.dao.ReferencesDao
import java.util.*

@RestController
@RequestMapping("/api/{bsId}/references")
class ReferencesController (private val referencesDao: ReferencesDao) {
    @RequestMapping("currencies")
    @Transactional
    fun getCurrencies(@PathVariable bsId: UUID): HmResponse {
        val currencies = referencesDao.getCurrencies(bsId)
        return getOk(currencies)
    }
}