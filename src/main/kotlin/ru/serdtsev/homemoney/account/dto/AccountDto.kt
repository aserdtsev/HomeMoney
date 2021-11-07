package ru.serdtsev.homemoney.account.dto

import ru.serdtsev.homemoney.account.model.AccountType
import java.sql.Date
import java.util.*

data class AccountDto(val id: UUID, val type: AccountType, val name: String, val createdDate: Date, val isArc: Boolean)