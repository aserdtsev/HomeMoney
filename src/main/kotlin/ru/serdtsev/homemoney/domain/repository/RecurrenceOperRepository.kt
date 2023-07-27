package ru.serdtsev.homemoney.domain.repository

import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.model.moneyoper.RecurrenceOper
import java.util.*

interface RecurrenceOperRepository {
    fun save(domainAggregate: RecurrenceOper)
    fun findById(id: UUID): RecurrenceOper
    fun findByIdOrNull(id: UUID): RecurrenceOper?
    fun exists(id: UUID): Boolean
    fun findByBalanceSheetAndArc(balanceSheet: BalanceSheet, isArc: Boolean? = null): List<RecurrenceOper>
}