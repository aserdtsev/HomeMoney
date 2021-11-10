package ru.serdtsev.homemoney.moneyoper.dao

import org.springframework.data.repository.CrudRepository
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.model.Tag
import java.util.*

interface TagRepo : CrudRepository<Tag?, UUID?> {
    fun findByBalanceSheetAndName(balanceSheet: BalanceSheet, name: String): Tag?
    fun findByBalanceSheetOrderByName(balanceSheet: BalanceSheet): List<Tag>
}