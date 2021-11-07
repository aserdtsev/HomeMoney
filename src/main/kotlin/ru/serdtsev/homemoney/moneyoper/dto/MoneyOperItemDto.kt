package ru.serdtsev.homemoney.moneyoper.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class MoneyOperItemDto (
        val id: UUID,
        val balanceId: UUID,
        val balanceName: String?,
        val value: BigDecimal,
        val sgn: Int,
        val currencyCode: String?,
        val performedAt: LocalDate?,
        val index: Int = 0
) {
    val currencySymbol: String
        get() = Currency.getInstance(currencyCode).symbol
}
