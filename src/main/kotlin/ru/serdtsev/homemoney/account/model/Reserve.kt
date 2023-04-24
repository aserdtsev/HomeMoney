package ru.serdtsev.homemoney.account.model

import ru.serdtsev.homemoney.account.dao.ReserveDao
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.Model
import ru.serdtsev.homemoney.moneyoper.service.MoneyOperService
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

open class Reserve(
    id: UUID,
    balanceSheet: BalanceSheet,
    name: String,
    createdDate: LocalDate = LocalDate.now(),
    currencyCode: String = balanceSheet.currencyCode,
    value: BigDecimal = BigDecimal.ZERO,
    open var target: BigDecimal = BigDecimal.ZERO,
    isArc: Boolean = false
) : Balance(id, balanceSheet, AccountType.reserve, name, createdDate, isArc, currencyCode, value) {
    internal constructor(balanceSheet: BalanceSheet, name: String, value: BigDecimal = BigDecimal.ZERO,
            target: BigDecimal = BigDecimal.ZERO) :
            this(UUID.randomUUID(), balanceSheet, name, value = value, target = target)

    companion object {
        fun merge(from: Reserve, to: Reserve): Collection<Model> {
            val changedModels = Balance.merge(from, to).plus(to)
            to.target = from.target
            return changedModels
        }
    }
}