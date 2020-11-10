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

@Suppress("CanBePrimaryConstructorProperty")
@Entity
@Table(name = "money_oper")
class MoneyOper(id: UUID, balanceSheet: BalanceSheet, status: MoneyOperStatus,
        performed: LocalDate = LocalDate.now(), dateNum: Int? = 0, labels: Collection<Label> = mutableListOf(),
        comment: String? = null, period: Period? = null) : Serializable {
    @Id
    val id = id

    @ManyToOne @JoinColumn(name = "balance_sheet_id")
    val balanceSheet = balanceSheet

    @Enumerated(EnumType.STRING)
    var status = status

    @OneToMany(cascade = [CascadeType.ALL]) @JoinColumn(name = "oper_id")
    val items: MutableList<MoneyOperItem> = mutableListOf()

    fun addItem(balance: Balance, value: BigDecimal, performed: LocalDate? = this.performed,
            index: Int = items.size, id: UUID = UUID.randomUUID()): MoneyOperItem {
        val item = MoneyOperItem(id, this, balance, value, performed, index)
        items.add(item)
        return item
    }

    @Column(name = "trn_date")
    var performed = performed

    @Enumerated(EnumType.STRING)
    var period = period

    @Column(name = "date_num")
    var dateNum = dateNum

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

    /**
     * Идентификатор повторяющейся операции. Служит для получения списка операций, которые были созданы по одному шаблону.
     */
    @Column(name = "recurrence_id")
    var recurrenceId: UUID? = null

    var comment: String? = comment
        get() = field.orEmpty()

    fun getParentOperId(): UUID? = parentOper?.id

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

    constructor(balanceSheet: BalanceSheet, status: MoneyOperStatus,
            performed: LocalDate = LocalDate.now(), dateNum: Int = 0, labels: Collection<Label> = mutableListOf(),
            comment: String? = null, period: Period? = null) : this(UUID.randomUUID(), balanceSheet, status, performed,
            dateNum, labels, comment, period)

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
        assert(other.id == this.id)
        return other.type == this.type && itemsEssentialEquals(other)
    }

    fun itemsEssentialEquals(other: MoneyOper): Boolean {
        return items.all { item -> other.items.any { it == item } }
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