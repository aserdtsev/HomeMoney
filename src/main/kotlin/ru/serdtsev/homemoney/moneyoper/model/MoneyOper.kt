package ru.serdtsev.homemoney.moneyoper.model

import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.Model
import java.io.Serializable
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

class MoneyOper (
    val id: UUID,
    val balanceSheet: BalanceSheet,
    var items: MutableList<MoneyOperItem> = mutableListOf(),
    var status: MoneyOperStatus,
    var performed: LocalDate = LocalDate.now(),
    var dateNum: Int? = 0,
    tags: Collection<Tag> = mutableListOf(),
    comment: String? = null,
    var period: Period? = null
) : Model, Serializable {
    var created: Timestamp = Timestamp.from(Instant.now().truncatedTo(ChronoUnit.MILLIS))
    val tags: MutableSet<Tag> = tags.toMutableSet()
    var parentOper: MoneyOper? = null
    /**
     * Идентификатор повторяющейся операции. Служит для получения списка операций, которые были созданы по одному шаблону.
     */
    var recurrenceId: UUID? = null

    var comment: String? = comment
        get() = field.orEmpty()

    val type: MoneyOperType
        get() {
            val hasReserve = items.any { it.balance.type == AccountType.reserve }
            if (!hasReserve) {
                val valueSignedSum = items.map { it.value.signum() }.sum()
                if (valueSignedSum > 0) {
                    return MoneyOperType.income
                } else if (valueSignedSum < 0) {
                    return MoneyOperType.expense
                }
            }
            return MoneyOperType.transfer
        }

    constructor(balanceSheet: BalanceSheet, status: MoneyOperStatus, performed: LocalDate? = LocalDate.now(),
        dateNum: Int? = 0, tags: Collection<Tag>? = mutableListOf(), comment: String? = null, period: Period? = Period.month
    ) : this(UUID.randomUUID(), balanceSheet, mutableListOf(), status, performed!!, dateNum, tags!!, comment, period)

    fun addItem(balance: Balance, value: BigDecimal, performed: LocalDate = this.performed,
            index: Int = items.size, id: UUID = UUID.randomUUID()): MoneyOperItem {
        val item = MoneyOperItem(id, this.id, balance, value, performed, index)
        items.add(item)
        return item
    }

    fun setTags(tags: Collection<Tag>) {
        this.tags.retainAll(tags.toSet())
        this.tags.addAll(tags)
    }

    fun getParentOperId(): UUID? = parentOper?.id

    val isForeignCurrencyTransaction: Boolean
        get() = items.any { it.balance.currencyCode != balanceSheet.currencyCode }

    val valueInNationalCurrency: BigDecimal
        get() = items
                .filter { it.balance.currencyCode == balanceSheet.currencyCode }
                .map { it.value }
                .reduce { acc, value -> acc.add(value) }

    fun complete() {
        assert(status == MoneyOperStatus.pending || status == MoneyOperStatus.cancelled) { status }
        assert(!performed.isAfter(LocalDate.now()))
        changeBalances(false)
        status = MoneyOperStatus.done
    }

    fun cancel() {
        assert(status == MoneyOperStatus.done || status == MoneyOperStatus.pending || status == MoneyOperStatus.template)
        if (status == MoneyOperStatus.done) {
            changeBalances(true)
        }
        status = MoneyOperStatus.cancelled
    }

    fun getBalances(): List<Balance> {
        return items.map { it.balance }
    }

    fun changeBalances(revert: Boolean) {
        val factor = BigDecimal.ONE.let { if (revert) it.negate() else it }
        items.forEach { item -> item.balance.changeValue(item.value * factor, this) }
    }

    override fun toString(): String {
        return "MoneyOper{" +
                "id=" + id +
                ", balanceSheet=" + balanceSheet +
                ", status=" + status +
                ", performed=" + performed +
                ", items=" + items +
                ", created=" + created +
                '}'
    }

    override fun merge(other: Any): Collection<Model> = merge(other as MoneyOper, this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MoneyOper

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        fun merge(from: MoneyOper, to: MoneyOper): Collection<Model> {
            assert(from.id == to.id)
            val changedModels = mutableSetOf<Model>(to)
            val prevStatus = from.status
            val mostlyEquals = balanceEquals(from, to)
            if (!mostlyEquals && to.status == MoneyOperStatus.done) {
                to.cancel()
            }
            changedModels.addAll(to.items.map { it.balance })

            mergeItems(from, to)
            to.performed = from.performed
            to.setTags(from.tags)
            to.dateNum = from.dateNum
            to.period = from.period
            to.comment = from.comment

            if (!mostlyEquals && prevStatus == MoneyOperStatus.done
                || to.status == MoneyOperStatus.pending && from.status == MoneyOperStatus.done
            ) {
                to.complete()
            }

            changedModels.addAll(to.items.map { it.balance })
            return changedModels;
        }

        private fun mergeItems(from: MoneyOper, to: MoneyOper) {
            to.items.forEach { item -> from.items.firstOrNull { it == item }?.let { MoneyOperItem.merge(it, item) } }
            to.items.removeIf { item -> from.items.none { it.id == item.id } }
            to.items.addAll(from.items.filter { item -> to.items.none { it.id == item.id } })
        }

        /**
         * Возвращает true, если экземпляры одной операции эквивалентны по проводкам
         */
        fun balanceEquals(a: MoneyOper, b: MoneyOper): Boolean {
            assert(b.id == a.id)
            return b.type == a.type
                    && a.items.all { item -> b.items.any { it.id == item.id && MoneyOperItem.balanceEquals(it, item) }
            }
        }
    }

}