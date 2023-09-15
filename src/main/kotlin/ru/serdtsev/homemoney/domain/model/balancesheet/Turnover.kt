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
    /** Корректировка свободных средств */
    var freeCorrection: BigDecimal = BigDecimal.ZERO,
    val isReal: Boolean
) {
    operator fun plus(other: Turnover) {
        amount += other.amount
        freeCorrection += other.freeCorrection
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Turnover

        if (operDate != other.operDate) return false
        if (turnoverType != other.turnoverType) return false
        if (isReal != other.isReal) return false

        return true
    }

    override fun hashCode(): Int {
        var result = operDate.hashCode()
        result = 31 * result + turnoverType.hashCode()
        result = 31 * result + isReal.hashCode()
        return result
    }
}