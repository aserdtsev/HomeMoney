package ru.serdtsev.homemoney.port.dto.account

import ru.serdtsev.homemoney.domain.model.account.AccountType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class BalanceDto(
    val id: UUID, val type: AccountType, val name: String, val createdDate: LocalDate, val isArc: Boolean = false,
    val value: BigDecimal = BigDecimal.ZERO, val currencyCode: String = "RUB", val currencySymbol: String = "",
    val minValue: BigDecimal = BigDecimal.ZERO, val creditLimit: BigDecimal?,
    val freeFunds: BigDecimal = BigDecimal.ZERO, val num: Long = 0L
)