package ru.serdtsev.homemoney.moneyoper.dao

import org.springframework.data.repository.CrudRepository
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.model.RecurrenceOper
import java.util.*

interface RecurrenceOperRepo : CrudRepository<RecurrenceOper, UUID> {
    fun findByBalanceSheet(balanceSheet: BalanceSheet): List<RecurrenceOper>
}