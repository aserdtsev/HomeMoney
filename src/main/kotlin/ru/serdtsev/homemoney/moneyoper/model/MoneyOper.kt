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

    override fun merge(other: Any): Collection<Model> {
        other as MoneyOper
        assert(other.id == id)
        val changedModels = mutableSetOf<Model>(this)
        val prevStatus = status
        val mostlyEquals = mostlyEquals(other)
        if (!mostlyEquals && status == MoneyOperStatus.done) {
            cancel()
        }
        changedModels.addAll(items.map { it.balance })

        mergeItems(other.items)
        performed = other.performed
        setTags(other.tags)
        dateNum = other.dateNum
        period = other.period
        comment = other.comment

        if (!mostlyEquals && prevStatus == MoneyOperStatus.done
            || status == MoneyOperStatus.pending && other.status == MoneyOperStatus.done
        ) {
            complete()
        }

        changedModels.addAll(items.map { it.balance })
        return changedModels;
    }

    private fun mergeItems(otherItems: List<MoneyOperItem>) {
        items.forEach { item -> otherItems.firstOrNull { it == item }?.let { item.merge(it) } }
        items.removeIf { item -> otherItems.none { it.id == item.id } }
        items.addAll(otherItems.filter { item -> this.items.none { it.id == item.id } })
    }

    fun mostlyEquals(other: MoneyOper): Boolean {
        assert(other.id == this.id)
        return other.type == this.type
                && items.all { item -> other.items.any { it.id == item.id && it.mostlyEquals(item) }
        }
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

}