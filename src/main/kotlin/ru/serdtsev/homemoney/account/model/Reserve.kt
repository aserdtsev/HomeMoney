package ru.serdtsev.homemoney.account.model

import ru.serdtsev.homemoney.account.ReserveRepo
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.service.MoneyOperService
import java.math.BigDecimal
import java.sql.Date
import java.util.*
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "reserve")
@DiscriminatorValue("reserve")
open class Reserve(
    id: UUID,
    balanceSheet: BalanceSheet,
    name: String,
    createdDate: Date,
    currencyCode: String,
    value: BigDecimal,
    open var target: BigDecimal = BigDecimal.ZERO,
    isArc: Boolean? = null
) : Balance(id, balanceSheet, AccountType.reserve, name, createdDate, isArc, value, currencyCode) {
    fun merge(reserve: Reserve, reserveRepo: ReserveRepo, moneyOperService: MoneyOperService) {
        super.merge(reserve, reserveRepo, moneyOperService)
        target = reserve.target
    }
}