package ru.serdtsev.homemoney.dto

import java.sql.Timestamp
import java.util.*

data class BalanceSheet(
    var id: UUID? = null,
    var defaultCurrencyCode: String? = null,
    var createdTs: Timestamp? = null,
    var svcRsvId: UUID? = null,
    var uncatCostsId: UUID? = null,
    var uncatIncomeId: UUID? = null) {
}
