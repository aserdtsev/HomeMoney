package ru.serdtsev.homemoney.moneyoper.model

import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.Model
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
) : Model, Serializable {
    val balanceSheet: BalanceSheet = balance.balanceSheet

    fun mostlyEquals(other: MoneyOperItem): Boolean {
        assert(other.id == id)
        return moneyOperId == other.moneyOperId && balance == other.balance && value.compareTo(other.value) == 0
    }

    override fun merge(other: Any): Collection<Model> {
        other as MoneyOperItem
        assert(other.id == id && other.moneyOperId == moneyOperId)
        this.balance = other.balance
        this.value = other.value
        this.index = other.index
        this.performed = other.performed
        return listOf(this)
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MoneyOperItem

        if (id != other.id) return false
        if (moneyOperId != other.moneyOperId) return false
        if (balanceSheet != other.balanceSheet) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + moneyOperId.hashCode()
        result = 31 * result + balanceSheet.hashCode()
        return result
    }
}