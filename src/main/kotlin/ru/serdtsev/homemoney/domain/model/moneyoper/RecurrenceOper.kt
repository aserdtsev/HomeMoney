package ru.serdtsev.homemoney.domain.model.moneyoper

import ru.serdtsev.homemoney.domain.event.DomainEvent
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import java.io.Serializable
import java.time.LocalDate
import java.util.*

data class RecurrenceOper(
    val id: UUID,
    val balanceSheet: BalanceSheet,
    val template: MoneyOper,
    var nextDate: LocalDate,
    var arc: Boolean = false
) : DomainEvent, Serializable {
    constructor(balanceSheet: BalanceSheet, template: MoneyOper, nextDate: LocalDate) :
            this(UUID.randomUUID(), balanceSheet, template, nextDate)

    fun skipNextDate(): LocalDate {
        nextDate = calcNextDate(nextDate)
        DomainEventPublisher.instance.publish(this)
        return nextDate
    }

    fun calcNextDate(date: LocalDate): LocalDate = when (template.period) {
        Period.month -> date.plusMonths(1)
        Period.quarter -> date.plusMonths(3)
        Period.year -> date.plusYears(1)
        else -> date
    }

    /**
     * Переводит повторяющуюся операцию в архив.
     */
    fun arc() {
        template.cancel()
        arc = true
    }
}