package ru.serdtsev.homemoney.moneyoper.model

import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class MoneyOperItem(
        val id: UUID,
        val moneyOperId: UUID,
        var balance: Balance,
        var value: BigDecimal,
        var performed: LocalDate,
        var index: Int
) : Serializable {
    val balanceSheet: BalanceSheet = balance.balanceSheet

    fun mostlyEquals(other: MoneyOperItem): Boolean {
        assert(equals(other))
        return moneyOperId == other.moneyOperId && balance == other.balance && value.compareTo(other.value) == 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MoneyOperItem

        if (id != other.id) return false
        if (moneyOperId != other.moneyOperId) return false
        if (balance != other.balance) return false
        if (value != other.value) return false
        if (performed != other.performed) return false
        if (index != other.index) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + moneyOperId.hashCode()
        result = 31 * result + balance.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + performed.hashCode()
        result = 31 * result + index
        return result
    }

    override fun toString(): String {
        return "MoneyOperItem{" +
                "id=" + id +
                ", moneyOperId=" + moneyOperId +
                ", balanceId=" + balance.id +
                ", value=" + value +
                ", performed=" + performed +
                ", index=" + index +
                '}'
    }
}