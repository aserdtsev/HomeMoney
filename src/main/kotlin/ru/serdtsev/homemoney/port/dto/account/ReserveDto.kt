package ru.serdtsev.homemoney.port.dto.account

import ru.serdtsev.homemoney.domain.model.account.AccountType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class ReserveDto(
    val id: UUID, val type: AccountType, val name: String, val createdDate: LocalDate, val isArc: Boolean,
    val currencyCode: String, val value: BigDecimal, val target: BigDecimal
)