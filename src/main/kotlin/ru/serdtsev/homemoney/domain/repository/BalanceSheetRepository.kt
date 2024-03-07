package ru.serdtsev.homemoney.domain.repository

import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import java.math.BigDecimal
import java.util.*

interface BalanceSheetRepository {
    fun deleteById(id: UUID)
    fun exists(id: UUID): Boolean
    fun findById(id: UUID): BalanceSheet
    fun findByIdOrNull(id: UUID): BalanceSheet?
    fun getAggregateAccountSaldoList(): List<Pair<AccountType, BigDecimal>>
    fun getActualDebt(): BigDecimal
    fun findAll(): List<BalanceSheet>
}