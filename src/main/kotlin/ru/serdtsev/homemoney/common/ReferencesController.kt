package ru.serdtsev.homemoney.common

import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.common.HmResponse.Companion.getOk
import ru.serdtsev.homemoney.common.dao.ReferencesDao

@RestController
@RequestMapping("/api/references")
class ReferencesController (
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val referencesDao: ReferencesDao) {
    @RequestMapping("currencies")
    @Transactional
    fun getCurrencies(): HmResponse {
        val currencies = referencesDao.getCurrencies(apiRequestContextHolder.getBsId())
        return getOk(currencies)
    }
}