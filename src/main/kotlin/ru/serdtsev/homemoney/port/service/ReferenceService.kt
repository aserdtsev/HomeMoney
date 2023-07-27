package ru.serdtsev.homemoney.port.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.domain.model.HmCurrency
import ru.serdtsev.homemoney.domain.repository.ReferenceRepository
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder

@Service
class ReferenceService(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val referencesRepository: ReferenceRepository
) {
    @Transactional(readOnly = true)
    fun getCurrencies(): List<HmCurrency> {
        return referencesRepository.getCurrencies(apiRequestContextHolder.getBsId())
    }
}