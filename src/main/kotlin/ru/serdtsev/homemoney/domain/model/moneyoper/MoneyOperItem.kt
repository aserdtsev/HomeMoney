package ru.serdtsev.homemoney.domain.model.moneyoper

import ru.serdtsev.homemoney.domain.event.DomainEvent
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.account.Credit
import ru.serdtsev.homemoney.domain.repository.RepositoryRegistry
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class MoneyOperItem (
    val id: UUID,
    val moneyOperId: UUID,
    var balanceId: UUID,
    var value: BigDecimal,
    var performed: LocalDate,
    var index: Int,
    var repaymentSchedule: RepaymentSchedule? = null
) : DomainEvent, Serializable {
    val balance: Balance
        get() = requireNotNull(RepositoryRegistry.instance.balanceRepository.findById(balanceId))

    val dateWithGracePeriod: LocalDate
        get() = repaymentSchedule?.first()?.endDate ?: performed

    val isDebtRepayment: Boolean
        get() = balance.isCreditCard && value > BigDecimal.ZERO

    override fun toString(): String {
        return "MoneyOperItem{id=$id, moneyOperId=$moneyOperId, balanceId=$balanceId, value=$value, " +
                "performed=$performed, repaymentSchedule=$repaymentSchedule, index=$index"
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
        fun of(moneyOperId: UUID, balance: Balance, value: BigDecimal, performed: LocalDate, index: Int,
            id: UUID = UUID.randomUUID()): MoneyOperItem {
            val repaymentSchedule = if (value < 0.toBigDecimal())
                balance.credit?.let { RepaymentSchedule.of(performed, it, value.abs()) }
                else null
            return MoneyOperItem(id, moneyOperId, balance.id, value, performed, index, repaymentSchedule)
        }

        fun balanceEquals(a: MoneyOperItem, b: MoneyOperItem): Boolean {
            assert(b.id == a.id)
            return a.moneyOperId == b.moneyOperId && a.balanceId == b.balanceId && a.value.compareTo(b.value) == 0
        }

        fun merge(from: MoneyOperItem, to: MoneyOperItem): Collection<DomainEvent> {
            assert(from.id == to.id && from.moneyOperId == to.moneyOperId)
            to.balanceId = from.balanceId
            to.value = from.value
            to.index = from.index
            to.performed = from.performed
            return listOf(to)
        }
    }
}

class RepaymentSchedule : ArrayList<RepaymentScheduleItem>() {
    companion object {
        fun of(vararg items: RepaymentScheduleItem) = RepaymentSchedule().apply { addAll(items) }
        fun of(date: LocalDate, credit: Credit, mainDebtAmount: BigDecimal, interestAmount: BigDecimal = BigDecimal("0.00")) =
            RepaymentScheduleItem.of(date, credit, mainDebtAmount, interestAmount)?.let { of(it) }
    }
}

data class RepaymentScheduleItem(
    var startDate: LocalDate,
    var endDate: LocalDate,
    var totalAmount: BigDecimal,
    var mainDebtAmount: BigDecimal,
    var interestAmount: BigDecimal,
    /** ID элемента операции гашения */
    var repaymentDebtOperItemId: UUID? = null,
    /** Сколько погашено */
    var repaidDebtAmount: BigDecimal? = null
) {
    companion object {
        fun of(date: LocalDate, credit: Credit, mainDebtAmount: BigDecimal,
                interestAmount: BigDecimal = BigDecimal.ZERO): RepaymentScheduleItem? {
            val (estimatedDay, repaymentDay) = with (credit) {
                if (this.estimatedDay == null || this.repaymentDay == null) {
                    return null
                }
                requireNotNull(this.estimatedDay).toLong() to requireNotNull(this.repaymentDay)
            }
            val startDate = if (date.dayOfMonth > estimatedDay)
                date.minusDays(date.dayOfMonth - estimatedDay - 1).plusMonths(1)
                else date.plusDays(estimatedDay - date.dayOfMonth + 1)
            val endDate = startDate.withDayOfMonth(repaymentDay).plusMonths(1)
            val totalAmount = mainDebtAmount + interestAmount
            return RepaymentScheduleItem(startDate, endDate, totalAmount, mainDebtAmount, interestAmount)
        }
    }
}