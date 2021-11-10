package ru.serdtsev.homemoney.reference.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.reference.dao.ReferenceDao
import ru.serdtsev.homemoney.reference.model.HmCurrency

@Service
class ReferenceService(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val referencesDao: ReferenceDao
) {
    @Transactional(readOnly = true)
    fun getCurrencies(): List<HmCurrency> {
        return referencesDao.getCurrencies(apiRequestContextHolder.getBsId())
    }
}