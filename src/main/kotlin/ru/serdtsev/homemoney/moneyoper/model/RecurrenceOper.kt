package ru.serdtsev.homemoney.moneyoper.model

import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import java.time.LocalDate
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "recurrence_oper")
class RecurrenceOper(
        @Id
        val id: UUID,
        @ManyToOne @JoinColumn(name = "balance_sheet_id")
        val balanceSheet: BalanceSheet,
        @OneToOne @JoinColumn(name = "template_id")
        val template: MoneyOper,
        @Column(name = "next_date") var nextDate: LocalDate
) {
    @Column(name = "is_arc")
    var arc: Boolean = false
        private set

    constructor(balanceSheet: BalanceSheet, template: MoneyOper, nextDate: LocalDate) :
            this(UUID.randomUUID(), balanceSheet, template, nextDate)

    fun skipNextDate(): LocalDate {
        nextDate = calcNextDate(nextDate)
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecurrenceOper) return false
        return balanceSheet == other.balanceSheet && template == other.template
    }

    override fun hashCode(): Int {
        return Objects.hash(balanceSheet, template)
    }

    override fun toString(): String {
        return "RecurrenceOper{" +
                "id=" + id +
                ", balanceSheet=" + balanceSheet +
                ", template=" + template +
                ", nextDate=" + nextDate +
                '}'
    }
}