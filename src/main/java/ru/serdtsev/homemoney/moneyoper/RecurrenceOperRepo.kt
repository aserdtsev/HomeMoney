package ru.serdtsev.homemoney.moneyoper

import org.springframework.data.repository.CrudRepository
import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.model.RecurrenceOper
import java.util.*

interface RecurrenceOperRepo : CrudRepository<RecurrenceOper, UUID> {
    fun findByBalanceSheet(balanceSheet: BalanceSheet): List<RecurrenceOper>
}