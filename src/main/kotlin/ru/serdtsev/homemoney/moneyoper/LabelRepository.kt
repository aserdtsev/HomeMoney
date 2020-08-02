package ru.serdtsev.homemoney.moneyoper

import org.springframework.data.repository.CrudRepository
import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.model.Label
import java.util.*

interface LabelRepository : CrudRepository<Label?, UUID?> {
    fun findByBalanceSheetAndName(balanceSheet: BalanceSheet, name: String): Label?
    fun findByBalanceSheetOrderByName(balanceSheet: BalanceSheet): List<Label>
}