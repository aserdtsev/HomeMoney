package ru.serdtsev.homemoney.domain.model.account

import mu.KotlinLogging
import ru.serdtsev.homemoney.domain.event.DomainEvent
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import ru.serdtsev.homemoney.domain.model.moneyoper.Period
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*

open class Balance(
    id: UUID,
    type: AccountType,
    name: String,
    createdDate: LocalDate = LocalDate.now(),
    isArc: Boolean = false,
    open var currencyCode: String = ApiRequestContextHolder.balanceSheet.currencyCode,
    value: BigDecimal = BigDecimal.ZERO,
    minValue: BigDecimal = BigDecimal.ZERO,
    var credit: Credit? = null
) : Account(id, type, name, createdDate, isArc), DomainEvent {
    open var value: BigDecimal = value.setScale(getCurrencyFractionDigits(), RoundingMode.HALF_UP)
        set(value) {
            field = value.setScale(getCurrencyFractionDigits(), RoundingMode.HALF_UP)
        }

    open var minValue: BigDecimal = minValue.setScale(getCurrencyFractionDigits(), RoundingMode.HALF_UP)
        set(value) {
            field = value.setScale(getCurrencyFractionDigits(), RoundingMode.HALF_UP)
        }

    open var reserve: Reserve? = null
    open var num: Long? = null
        get() = field ?: 0L

    open val reserveId: UUID?
        get() = reserve?.id
    
    internal constructor(type: AccountType, name: String, value: BigDecimal = BigDecimal.ZERO) :
            this(UUID.randomUUID(), type, name, value = value)

    private fun getCurrencyFractionDigits() = ApiRequestContextHolder.balanceSheet.getCurrencyFractionDigits()

    open val currencySymbol: String
        get() = currency.symbol

    open val currency: Currency
        get() = Currency.getInstance(currencyCode)

    open fun changeValue(amount: BigDecimal, moneyOperId: UUID) {
        val beforeValue = value.plus()
        value += amount
        log.info("Balance value changed; id: $id, operId: ${moneyOperId}, " +
                "before: $beforeValue, amount: $amount, after: $value")
    }

    open val freeFunds: BigDecimal
        get() = value + (credit?.creditLimit ?: BigDecimal.ZERO) - minValue

    override fun toString(): String {
        return """
            Balance(id=$id, type=$type, name='$name', createdDate=$createdDate, isArc=$isArc, currencyCode='$currencyCode', 
            value=$value, minValue=$minValue, credit=$credit, num=$num, reserveId=$reserveId)
            """.trimIndent()
    }

    companion object {
        private val log = KotlinLogging.logger {  }

        fun merge(from: Balance, to: Balance) {
            to.name = from.name
            to.credit = from.credit
            to.minValue = from.minValue
            to.reserve = from.reserve
            if (from.value.compareTo(to.value) != 0) {
                if (from.type == AccountType.reserve) {
                    to.value = from.value
                } else {
                    val moneyOper = MoneyOper(MoneyOperStatus.pending, LocalDate.now(),
                        0, emptyList(), "корректировка остатка", Period.single)
                    val amount = from.value - to.value
                    moneyOper.addItem(to, amount, moneyOper.performed)
                    moneyOper.complete()
                    DomainEventPublisher.instance.publish(moneyOper)
                }
            }
            DomainEventPublisher.instance.publish(to)
        }
    }
}

data class Credit(var creditLimit: BigDecimal? = null, var annuityPayment: AnnuityPayment? = null)

data class AnnuityPayment(var value: BigDecimal)