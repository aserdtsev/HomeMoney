package ru.serdtsev.homemoney.account.model

import mu.KotlinLogging
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.Model
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import ru.serdtsev.homemoney.moneyoper.model.Period
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*

open class Balance(
    id: UUID,
    balanceSheet: BalanceSheet,
    type: AccountType,
    name: String,
    createdDate: LocalDate = LocalDate.now(),
    isArc: Boolean = false,
    open var currencyCode: String = balanceSheet.currencyCode,
    value: BigDecimal = BigDecimal.ZERO,
    minValue: BigDecimal = BigDecimal.ZERO,
    var credit: Credit? = null
) : Account(id, balanceSheet, type, name, createdDate, isArc), Model {
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
    
    internal constructor(balanceSheet: BalanceSheet, type: AccountType, name: String, value: BigDecimal = BigDecimal.ZERO) :
            this(UUID.randomUUID(), balanceSheet, type, name, value = value)

    private fun getCurrencyFractionDigits() = balanceSheet.getCurrencyFractionDigits()

    override fun merge(other: Any): Collection<Model> {
        return listOf()
    }

    open val currencySymbol: String
        get() = currency.symbol

    open val currency: Currency
        get() = Currency.getInstance(currencyCode)

    open fun changeValue(amount: BigDecimal, oper: MoneyOper) {
        val beforeValue = value.plus()
        value += amount
        log.info("Balance value changed; id: $id, operId: ${oper.id}, beforeStatus: ${oper.status.name}, " +
                "before: $beforeValue, amount: $amount, after: $value")
    }

    open val freeFunds: BigDecimal
        get() = value + (credit?.creditLimit ?: BigDecimal.ZERO) - minValue

    override fun toString(): String {
        return "Balance(id=$id, balanceSheetId=${balanceSheet.id}, type=$type, name='$name', createdDate=$createdDate, " +
                "isArc=$isArc, currencyCode='$currencyCode', value=$value, minValue=$minValue, credit=$credit, " +
                "num=$num, reserveId=$reserveId)"
    }

    companion object {
        private val log = KotlinLogging.logger {  }

        fun merge(from: Balance, to: Balance): Collection<Model> {
            val changedModels = Account.merge(from, to).plus(to).toMutableList()
            to.credit = from.credit
            to.minValue = from.minValue
            to.reserve = from.reserve
            if (from.value.compareTo(to.value) != 0) {
                if (from.type == AccountType.reserve) {
                    to.value = from.value
                } else {
                    val moneyOper = MoneyOper(to.balanceSheet, MoneyOperStatus.pending, LocalDate.now(), 0, emptyList(),
                        "корректировка остатка", Period.single)
                    val amount = from.value - to.value
                    moneyOper.addItem(to, amount, moneyOper.performed)
                    moneyOper.complete()
                    changedModels.add(moneyOper)
                }
            }
            return changedModels
        }
    }
}

data class Credit(var creditLimit: BigDecimal? = null, var annuityPayment: AnnuityPayment? = null)

data class AnnuityPayment(var value: BigDecimal)