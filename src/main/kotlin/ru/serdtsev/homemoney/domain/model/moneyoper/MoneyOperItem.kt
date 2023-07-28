package ru.serdtsev.homemoney.domain.model.moneyoper

import ru.serdtsev.homemoney.domain.event.DomainEvent
import ru.serdtsev.homemoney.domain.model.account.Balance
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

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
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