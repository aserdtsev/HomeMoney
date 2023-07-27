package ru.serdtsev.homemoney.domain.repository

import ru.serdtsev.homemoney.domain.model.HmCurrency
import java.util.*

interface ReferenceRepository {
    fun getCurrencies(bsId: UUID): List<HmCurrency>
}