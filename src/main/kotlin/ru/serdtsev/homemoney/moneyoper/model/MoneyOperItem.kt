package ru.serdtsev.homemoney.moneyoper.model

import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import javax.persistence.*

@Suppress("JpaQlInspection")
@Entity
@Table(name = "money_oper_item")
@NamedQuery(name = "MoneyOperItem.findByBalanceSheetAndValueOrderByPerformedDesc",
        query = "select m from MoneyOperItem m where balanceSheet = ?1 and abs(value) = ?2 order by performed desc")
class MoneyOperItem(
        @Id val id: UUID,

        @ManyToOne @JoinColumn(name = "oper_id")
        var moneyOper: MoneyOper,

        @ManyToOne
        @JoinColumn(name = "balance_id")
        var balance: Balance,

        var value: BigDecimal,
        var performed: LocalDate? = null,
        var index: Int
) : Serializable {
    @ManyToOne @JoinColumn(name = "bs_id") val balanceSheet: BalanceSheet = balance.balanceSheet!!
    fun getBalanceId(): UUID = balance.id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MoneyOperItem) return false
        return moneyOper.id == other.moneyOper.id &&
                balance.id == other.balance.id && value.compareTo(other.value) == 0
    }

    override fun hashCode(): Int {
        return Objects.hash(moneyOper, balance, value)
    }

    fun essentialEquals(other: MoneyOperItem): Boolean {
        assert(equals(other))
        return moneyOper == other.moneyOper && balance == other.balance && value.compareTo(other.value) == 0
    }

    override fun toString(): String {
        return "MoneyOperItem{" +
                "id=" + id +
                ", moneyOperId=" + moneyOper.id +
                ", balanceId=" + balance.id +
                ", value=" + value +
                ", performed=" + performed +
                ", index=" + index +
                '}'
    }
}