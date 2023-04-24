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

    override fun merge(other: Any): Collection<Model> {
        TODO("Удалить")
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

    companion object {
        fun balanceEquals(a: MoneyOperItem, b: MoneyOperItem): Boolean {
            assert(b.id == a.id)
            return a.moneyOperId == b.moneyOperId && a.balance == b.balance && a.value.compareTo(b.value) == 0
        }

        fun merge(from: MoneyOperItem, to: MoneyOperItem): Collection<Model> {
            assert(from.id == to.id && from.moneyOperId == to.moneyOperId)
            to.balance = from.balance
            to.value = from.value
            to.index = from.index
            to.performed = from.performed
            return listOf(to)
        }
    }
}