package ru.serdtsev.homemoney.moneyoper

import org.springframework.data.repository.CrudRepository
import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.model.Tag
import java.util.*

interface TagRepository : CrudRepository<Tag?, UUID?> {
    fun findByBalanceSheetAndName(balanceSheet: BalanceSheet, name: String): Tag?
    fun findByBalanceSheetOrderByName(balanceSheet: BalanceSheet): List<Tag>
}