package ru.serdtsev.homemoney.balancesheet

import ru.serdtsev.homemoney.account.model.AccountType
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Обороты за день по типу счета
 */
data class Turnover(
        val operDate: LocalDate,
        val accountType: AccountType,
        /** Сумма оборотов со знаком */
        var amount: BigDecimal = BigDecimal.ZERO
) {
    operator fun plus(value: BigDecimal) { amount = amount.add(value) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Turnover

        if (operDate != other.operDate) return false
        if (accountType != other.accountType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = operDate.hashCode()
        result = 31 * result + accountType.hashCode()
        return result
    }
}
