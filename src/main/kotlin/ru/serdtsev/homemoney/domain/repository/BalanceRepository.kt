package ru.serdtsev.homemoney.domain.repository

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import java.util.*

interface BalanceRepository {
    fun delete(balance: Balance)
    fun exists(id: UUID): Boolean
    fun findById(id: UUID): Balance
    fun findByIdOrNull(id: UUID): Balance?
    fun findByBalanceSheet(balanceSheet: BalanceSheet): List<Balance>
}