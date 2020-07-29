package ru.serdtsev.homemoney.account.model

import ru.serdtsev.homemoney.account.ReserveRepository
import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.MoneyOperService
import java.math.BigDecimal
import java.sql.Date
import java.util.*
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "reserves")
@DiscriminatorValue("reserve")
open class Reserve(
        id: UUID,
        balanceSheet: BalanceSheet,
        name: String,
        created: Date,
        currencyCode: String,
        value: BigDecimal,
        open var target: BigDecimal = BigDecimal.ZERO,
        isArc: Boolean? = null
) : Balance(id, balanceSheet, AccountType.reserve, name, created, isArc, currencyCode, value) {
    fun merge(reserve: Reserve, reserveRepo: ReserveRepository, moneyOperService: MoneyOperService) {
        super.merge(reserve, reserveRepo, moneyOperService)
        target = reserve.target
    }
}