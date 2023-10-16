package ru.serdtsev.homemoney.domain.model.moneyoper

import ru.serdtsev.homemoney.domain.event.DomainEvent
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus.*
import ru.serdtsev.homemoney.domain.repository.RepositoryRegistry
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
        period: Period? = Period.month,
        recurrenceId: UUID? = null,
        dateNum: Int = 0
    ) : this(UUID.randomUUID(), mutableListOf(), status, performed!!, tags!!, comment, period, recurrenceId, dateNum)

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
        status = new
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
        assert(status in listOf(new, pending, cancelled)) { status }
        assert(!performed.isAfter(LocalDate.now()))
        val beforeStatus = status
        status = done
        DomainEventPublisher.instance.publish(this)
        val statusChanged = MoneyOperStatusChanged(beforeStatus, status, this)
        DomainEventPublisher.instance.publish(statusChanged)
    }

    fun postpone() {
        assert(status in listOf(done, pending)) { status }
        val beforeStatus = status
        status = pending
        DomainEventPublisher.instance.publish(this)
        if (beforeStatus != pending) {
            val statusChanged = MoneyOperStatusChanged(beforeStatus, status, this)
            DomainEventPublisher.instance.publish(statusChanged)
        }
    }

    fun cancel() {
        assert(status in listOf(done,  pending, template))
        val beforeStatus = status
        status = cancelled
        DomainEventPublisher.instance.publish(this)
        val statusChanged = MoneyOperStatusChanged(beforeStatus, status, this)
        DomainEventPublisher.instance.publish(statusChanged)
    }

    fun skipPending() {
        recurrenceId = null
        cancel()
    }

    fun createTemplate(): MoneyOper {
        val template = MoneyOper(template, performed, tags, comment, period, dateNum = 0)
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
        fun merge(from: MoneyOper, to: MoneyOper) {
            assert(from.id == to.id)
            val balanceEquals = balanceEquals(from, to)
            if (to.status == done && !balanceEquals) {
                to.cancel()
            }
            if (from.status == done && to.performed.isAfter(from.performed)) {
                to.postpone()
            }

            mergeItems(from, to)
            to.performed = from.performed
            to.setTags(from.tags)
            to.dateNum = from.dateNum
            to.period = from.period
            to.comment = from.comment

            if (from.status == done && !balanceEquals || to.status == pending && from.status == done) {
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