package ru.serdtsev.homemoney.account.dto

import ru.serdtsev.homemoney.account.model.AccountType
import java.math.BigDecimal
import java.sql.Date
import java.util.*

data class BalanceDto(
        val id: UUID, val type: AccountType, val name: String, val createdDate: Date, val isArc: Boolean,
        val value: BigDecimal, val currencyCode: String, val currencySymbol: String, val minValue: BigDecimal,
        val creditLimit: BigDecimal, val freeFunds: BigDecimal, val num: Long
)