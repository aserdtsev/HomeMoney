package ru.serdtsev.homemoney.domain.repository

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import java.math.BigDecimal
import java.util.*

interface BalanceSheetRepository {
    fun deleteById(id: UUID)
    fun exists(id: UUID): Boolean
    fun findById(id: UUID): BalanceSheet
    fun findByIdOrNull(id: UUID): BalanceSheet?
    fun getAggregateAccountSaldoList(id: UUID): List<Pair<AccountType, BigDecimal>>
    fun getActualDebt(id: UUID): BigDecimal
    fun findAll(): List<BalanceSheet>
}