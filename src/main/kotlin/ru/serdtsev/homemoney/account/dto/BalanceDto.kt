package ru.serdtsev.homemoney.account.dto

import ru.serdtsev.homemoney.account.model.AccountType
import java.math.BigDecimal
import java.sql.Date
import java.time.LocalDate
import java.util.*

data class BalanceDto(
        val id: UUID, val type: AccountType, val name: String, val createdDate: LocalDate, val isArc: Boolean = false,
        val value: BigDecimal = BigDecimal.ZERO, val currencyCode: String = "RUB", val currencySymbol: String = "",
        val minValue: BigDecimal = BigDecimal.ZERO, val creditLimit: BigDecimal = BigDecimal.ZERO,
        val freeFunds: BigDecimal = BigDecimal.ZERO, val num: Long = 0L
)