package ru.serdtsev.homemoney.domain.repository

import ru.serdtsev.homemoney.domain.model.account.Reserve
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import java.util.*

interface ReserveRepository {
    fun save(domainAggregate: Reserve)
    fun delete(reserve: Reserve)
    fun exists(id: UUID): Boolean
    fun findById(id: UUID): Reserve
    fun findByIdOrNull(id: UUID): Reserve?
    fun findByBalanceSheet(balanceSheet: BalanceSheet): List<Reserve>
}