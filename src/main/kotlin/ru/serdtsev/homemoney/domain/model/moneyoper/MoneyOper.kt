package ru.serdtsev.homemoney.domain.model.moneyoper

import ru.serdtsev.homemoney.domain.event.DomainEvent
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus.*
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import java.io.Serializable
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

class MoneyOper(
    val id: UUID,
    var items: MutableList<MoneyOperItem> = mutableListOf(),
    var status: MoneyOperStatus,
    var performed: LocalDate = LocalDate.now(),
    tags: Collection<Tag> = mutableListOf(),
    comment: String? = null,
    var period: Period? = null,
    var recurrenceParams: RecurrenceParams? = null,
    aRecurrenceId: UUID? = null,
    var dateNum: Int = 0
) : DomainEvent, Serializable {
    var created: Timestamp = Timestamp.from(Instant.now().truncatedTo(ChronoUnit.MILLIS))
    val tags: MutableSet<Tag> = tags.toMutableSet()
    var parentOper: MoneyOper? = null
    /**
     * Идентификатор повторяющейся операции. Служит для получения списка операций, которые были созданы по одному шаблону.
     */
    var recurrenceId: UUID? = aRecurrenceId
        private set

    var comment: String? = comment
        get() = field.orEmpty()

    val type: MoneyOperType
        get() {
            val hasReserve = items.any { it.balance.type == AccountType.reserve }
            if (!hasReserve) {
                val valueSignedSum = items.sumOf { it.value.signum() }
                if (valueSignedSum > 0) {
                    return MoneyOperType.income
                } else if (valueSignedSum < 0) {
                    return MoneyOperType.expense
                }
            }
            return MoneyOperType.transfer
        }

    constructor(status: MoneyOperStatus,
        performed: LocalDate? = LocalDate.now(),
        tags: Collection<Tag>? = mutableListOf(),
        comment: String? = null,
        period: Period? = Period.Month,
        recurrenceParams: RecurrenceParams? = null,
        recurrenceId: UUID? = null,
        dateNum: Int = 0
    ) : this(UUID.randomUUID(), mutableListOf(), status, performed!!, tags!!, comment, period, recurrenceParams,
        recurrenceId, dateNum)

    fun addItem(balance: Balance, value: BigDecimal, performed: LocalDate = this.performed, index: Int = items.size,
            id: UUID = UUID.randomUUID()): MoneyOperItem {
        val item = MoneyOperItem.of(this.id, balance, value, performed, index, id)
        items.add(item)
        return item
    }

    fun setTags(tags: Collection<Tag>) {
        this.tags.retainAll(tags.toSet())
        this.tags.addAll(tags)
    }

    fun getParentOperId(): UUID? = parentOper?.id

    val isForeignCurrencyTransaction: Boolean
        get() = items.any { it.balance.currencyCode != ApiRequestContextHolder.balanceSheet.currencyCode }

    val valueInNationalCurrency: BigDecimal
        get() = items
                .filter { it.balance.currencyCode == ApiRequestContextHolder.balanceSheet.currencyCode }
                .map { it.value }
                .reduce { acc, value -> acc.add(value) }

    fun new() {
        status = New
    }

    fun newAndComplete() {
        new()
        complete()
    }

    fun newAndPostpone() {
        new()
        postpone()
    }

    fun complete() {
        assert(status in listOf(New, Pending, Cancelled)) { status }
        assert(!performed.isAfter(LocalDate.now()))
        val beforeStatus = status
        status = Done
        DomainEventPublisher.instance.publish(this)
        val statusChanged = MoneyOperStatusChanged(beforeStatus, status, this)
        DomainEventPublisher.instance.publish(statusChanged)
    }

    fun postpone() {
        assert(status in listOf(Done, Pending)) { status }
        val beforeStatus = status
        status = Pending
        DomainEventPublisher.instance.publish(this)
        if (beforeStatus != Pending) {
            val statusChanged = MoneyOperStatusChanged(beforeStatus, status, this)
            DomainEventPublisher.instance.publish(statusChanged)
        }
    }

    fun cancel() {
        assert(status in listOf(Done,  Pending, Template))
        val beforeStatus = status
        status = Cancelled
        DomainEventPublisher.instance.publish(this)
        val statusChanged = MoneyOperStatusChanged(beforeStatus, status, this)
        DomainEventPublisher.instance.publish(statusChanged)
    }

    fun skipPending() {
        recurrenceId = null
        cancel()
    }

    fun createTemplate(): MoneyOper {
        val template = MoneyOper(Template, performed, tags, comment, period, dateNum = 0)
        items.forEach { template.addItem(it.balance, it.value, it.performed) }
        return template
    }

    fun linkToRecurrenceOper(recurrenceOper: RecurrenceOper) {
        recurrenceId = recurrenceOper.id
    }

    override fun toString(): String {
        return "MoneyOper{" +
                "id=" + id +
                ", status=" + status +
                ", performed=" + performed +
                ", items=" + items +
                ", created=" + created +
                ", recurrenceParams=" + recurrenceParams
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

    companion object {
        fun of(status: MoneyOperStatus, period: Period, recurrenceParams: DayRecurrenceParams): MoneyOper {
            return MoneyOper(status, period = period, recurrenceParams = recurrenceParams)
        }

        fun merge(from: MoneyOper, to: MoneyOper) {
            assert(from.id == to.id)
            val balanceEquals = balanceEquals(from, to)
            if (to.status == Done && !balanceEquals) {
                to.cancel()
            }
            if (from.status == Done && to.performed.isAfter(from.performed)) {
                to.postpone()
            }

            mergeItems(from, to)
            to.performed = from.performed
            to.setTags(from.tags)
            to.dateNum = from.dateNum
            to.period = from.period
            to.comment = from.comment

            if (from.status == Done && !balanceEquals || to.status == Pending && from.status == Done) {
                to.complete()
            } else {
                DomainEventPublisher.instance.publish(to)
            }
        }

        private fun mergeItems(from: MoneyOper, to: MoneyOper) {
            to.items.forEach { item -> from.items.firstOrNull { it == item }?.let { MoneyOperItem.merge(it, item) } }
            to.items.removeIf { item -> from.items.none { it.id == item.id } }
            to.items.addAll(from.items.filter { item -> to.items.none { it.id == item.id } })
        }

        /**
         * Возвращает true, если экземпляры операции эквивалентны по проводкам
         */
        internal fun balanceEquals(a: MoneyOper, b: MoneyOper): Boolean {
            assert(b.id == a.id)
            return b.type == a.type
                    && a.items.all { item -> b.items.any { it.id == item.id && MoneyOperItem.balanceEquals(it, item) }
            }
        }
    }

}