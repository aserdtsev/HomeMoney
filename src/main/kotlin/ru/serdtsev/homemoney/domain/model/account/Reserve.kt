package ru.serdtsev.homemoney.domain.model.account

import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
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
        fun merge(from: Reserve, to: Reserve) {
            Balance.merge(from, to)
            to.target = from.target
            DomainEventPublisher.instance.publish(to)
        }
    }
}