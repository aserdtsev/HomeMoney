package ru.serdtsev.homemoney.port.dto.moneyoper

import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class MoneyOperItemDto (
        val id: UUID,
        var balanceId: UUID,
        var balanceName: String?,
        var value: BigDecimal,
        var sgn: Int,
        var currencyCode: String?,
        var performedAt: LocalDate,
        var index: Int = 0
) {
    val currencySymbol: String
        get() = Currency.getInstance(currencyCode).symbol
}
