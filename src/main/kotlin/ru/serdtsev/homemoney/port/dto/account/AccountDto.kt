package ru.serdtsev.homemoney.port.dto.account

import ru.serdtsev.homemoney.domain.model.account.AccountType
import java.time.LocalDate
import java.util.*

data class AccountDto(val id: UUID, val type: AccountType, val name: String, val createdDate: LocalDate, val isArc: Boolean)