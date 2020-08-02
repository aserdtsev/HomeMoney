package ru.serdtsev.homemoney.moneyoper.model

import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import java.io.Serializable
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.*
import java.util.function.Consumer
import javax.persistence.*

@Entity
@Table(name = "money_oper")
class MoneyOper(
        @Id val id: UUID,
        @ManyToOne @JoinColumn(name = "balance_sheet_id") val balanceSheet: BalanceSheet,
        @Enumerated(EnumType.STRING) var status: MoneyOperStatus?,
        performed: LocalDate,
        @Column(name = "date_num") var dateNum: Int?,
        labels: Collection<Label>,
        comment: String? = null,
        @Enumerated(EnumType.STRING) var period: Period? = null
) : Serializable {
    @Column(name = "trn_date")
    var performed: LocalDate = performed
        set(value) {
            field = value
            items.forEach { it.performed = value }
        }

    @OneToMany(cascade = [CascadeType.ALL])
    @JoinColumn(name = "oper_id")
    val items: MutableList<MoneyOperItem> = mutableListOf()

    @ManyToMany
    @JoinTable(name = "labels2objs",
            joinColumns = [JoinColumn(name = "obj_id", foreignKey = ForeignKey(value = ConstraintMode.NO_CONSTRAINT))],
            inverseJoinColumns = [JoinColumn(name = "label_id")])
    val labels: MutableSet<Label> = labels.toMutableSet()

    fun setLabels(labels: Collection<Label>) {
        this.labels.retainAll(labels)
        this.labels.addAll(labels)
    }

    @Column(name = "created_ts")
    var created: Timestamp = Timestamp.from(Instant.now())
        private set

    @OneToOne
    @JoinColumn(name = "parent_id")
    var parentOper: MoneyOper? = null

    @Deprecated("")
    @Column(name = "from_acc_id")
    lateinit var fromAccId: UUID

    @Deprecated("")
    @Column(name = "to_acc_id")
    lateinit var toAccId: UUID

    /**
     * Идентификатор повторяющейся операции. Служит для получения списка операций, которые были созданы по одному шаблону.
     */
    @Column(name = "recurrence_id")
    var recurrenceId: UUID? = null

    var comment: String? = comment
        get() = field.orEmpty()

    fun getParentOperId(): UUID? = parentOper?.id

    @Deprecated("")
    fun getAmount(): BigDecimal = items.first().value.abs()

    @Deprecated("")
    val currencyCode: String?
        get() = items
                .sortedBy { it.value.signum() }
                .map { it.balance.currencyCode }
                .firstOrNull()
                ?: balanceSheet.currencyCode

    @Deprecated("")
    fun getToAmount(): BigDecimal = items.first().value.abs()

    @Deprecated("")
    val toCurrencyCode: String?
        get() = items
                .sortedBy { it.value.signum() * -1 }
                .map { it.balance.currencyCode }
                .firstOrNull()
                ?: balanceSheet.currencyCode

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

    val isForeignCurrencyTransaction: Boolean
        get() = items.any { it.balance.currencyCode != balanceSheet.currencyCode }

    val valueInNationalCurrency: BigDecimal
        get() = items
                .filter { it.balance.currencyCode == balanceSheet.currencyCode }
                .map { it.value }
                .reduce { acc, value -> acc.add(value) }
                ?: BigDecimal.ZERO

    fun complete() {
        assert(status == MoneyOperStatus.pending || status == MoneyOperStatus.cancelled) { status!! }
        assert(!performed!!.isAfter(LocalDate.now()))
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

    fun addItems(items: Collection<MoneyOperItem>) {
        items.forEach { addItem(it.balance, it.value) }
    }

    @JvmOverloads
    fun addItem(balance: Balance, value: BigDecimal, performed: LocalDate = LocalDate.now()): MoneyOperItem {
        assert(value.compareTo(BigDecimal.ZERO) != 0) { this.toString() }
        val item = MoneyOperItem(UUID.randomUUID(), this, balance, value, performed, items.size)
        items.add(item)
        if (value.signum() == -1) {
            fromAccId = balance.id
        } else if (value.signum() == 1) {
            toAccId = balance.id
        }
        return item
    }

    fun changeBalances(revert: Boolean) {
        val factor = if (revert) BigDecimal.ONE.negate() else BigDecimal.ONE
        items.forEach(Consumer { item: MoneyOperItem -> item.balance.changeValue(item.value.multiply(factor), this) })
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val moneyOper = other as MoneyOper
        return id == moneyOper.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

    fun essentialEquals(other: MoneyOper): Boolean {
        assert(this == other)
        return itemsEssentialEquals(other)
    }

    fun itemsEssentialEquals(other: MoneyOper): Boolean {
        return items.all { item: MoneyOperItem -> other.items.any { i: MoneyOperItem -> i == item } }
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
}