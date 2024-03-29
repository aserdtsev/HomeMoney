package ru.serdtsev.homemoney.domain.model.balancesheet

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Обороты за день по типу счета
 */
data class Turnover(
    val operDate: LocalDate,
    val turnoverType: TurnoverType,
    /** Сумма оборота со знаком */
    var amount: BigDecimal = BigDecimal.ZERO,
    /** Изменение задолженности по кредитным картам */
    var creditCardDebtDelta: BigDecimal = BigDecimal.ZERO
) {
    operator fun plus(other: Turnover) {
        amount += other.amount
        creditCardDebtDelta += other.creditCardDebtDelta
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Turnover

        if (operDate != other.operDate) return false
        if (turnoverType != other.turnoverType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = operDate.hashCode()
        result = 31 * result + turnoverType.hashCode()
        return result
    }
}