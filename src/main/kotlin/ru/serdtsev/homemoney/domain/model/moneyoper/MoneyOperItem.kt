package ru.serdtsev.homemoney.domain.model.moneyoper

import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.event.DomainEvent
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
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
) : DomainEvent, Serializable {
    val balanceSheet: BalanceSheet = ApiRequestContextHolder.balanceSheet

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

    companion object {
        fun balanceEquals(a: MoneyOperItem, b: MoneyOperItem): Boolean {
            assert(b.id == a.id)
            return a.moneyOperId == b.moneyOperId && a.balance == b.balance && a.value.compareTo(b.value) == 0
        }

        fun merge(from: MoneyOperItem, to: MoneyOperItem): Collection<DomainEvent> {
            assert(from.id == to.id && from.moneyOperId == to.moneyOperId)
            to.balance = from.balance
            to.value = from.value
            to.index = from.index
            to.performed = from.performed
            return listOf(to)
        }
    }
}