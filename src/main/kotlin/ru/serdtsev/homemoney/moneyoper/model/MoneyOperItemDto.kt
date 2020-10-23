package ru.serdtsev.homemoney.moneyoper.model

import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class MoneyOperItemDto (
        val id: UUID,
        val balanceId: UUID,
        val balanceName: String?,
        val value: BigDecimal,
        val currencyCode: String,
        val performedAt: LocalDate?,
        val index: Int = 0
) {
    fun getCurrencySymbol() = Currency.getInstance(currencyCode).symbol
}
