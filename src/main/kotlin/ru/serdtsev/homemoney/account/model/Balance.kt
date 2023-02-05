package ru.serdtsev.homemoney.account.model

import mu.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import ru.serdtsev.homemoney.account.dao.ReserveDao
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import ru.serdtsev.homemoney.moneyoper.model.Period
import ru.serdtsev.homemoney.moneyoper.service.MoneyOperService
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
    var credit: Credit = Credit(BigDecimal.ZERO)
) : Account(id, balanceSheet, type, name, createdDate, isArc) {
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

    @Suppress("DuplicatedCode")
    fun merge(balance: Balance, reserveDao: ReserveDao, moneyOperService: MoneyOperService) {
        super.merge(balance)
        credit = balance.credit
        minValue = balance.minValue
        reserve = balance.reserveId?.let { reserveDao.findById(it) }
        if (balance.value.compareTo(value) != 0) {
            val balanceSheet = balanceSheet
            if (balance.type == AccountType.reserve) {
                value = balance.value
            } else {
                val moneyOper = MoneyOper(balanceSheet, MoneyOperStatus.pending, LocalDate.now(), 0, emptyList(),
                        "корректировка остатка", Period.single)
                val amount = balance.value - value
                moneyOper.addItem(this, amount, moneyOper.performed)
                moneyOper.complete()
                moneyOperService.save(moneyOper)
            }
        }
    }

    open val currencySymbol: String
        get() = currency.symbol

    open val currency: Currency
        get() = Currency.getInstance(currencyCode)

    @CacheEvict("BalanceDao.findById", key = "#id")
    open fun changeValue(amount: BigDecimal, oper: MoneyOper) {
        val beforeValue = value.plus()
        value += amount
        log.info("Balance value changed; id: $id, operId: ${oper.id}, beforeStatus: ${oper.status.name}, " +
                "before: $beforeValue, amount: $amount, after: $value")
    }

    open val freeFunds: BigDecimal
        get() = value + credit.creditLimit - minValue

    @Suppress("DuplicatedCode")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Balance) return false
        if (!super.equals(other)) return false

        if (currencyCode != other.currencyCode) return false
        if (value != other.value) return false
        if (minValue != other.minValue) return false
        if (reserve != other.reserve) return false
        if (credit != other.credit) return false
        if (num != other.num) return false
        if (reserveId != other.reserveId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + currencyCode.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + minValue.hashCode()
        result = 31 * result + credit.hashCode()
        result = 31 * result + (num?.hashCode() ?: 0)
        result = 31 * result + (reserveId?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Balance(id=$id, balanceSheetId=${balanceSheet.id}, type=$type, name='$name', createdDate=$createdDate, " +
                "isArc=$isArc, currencyCode='$currencyCode', value=$value, minValue=$minValue, credit=$credit, " +
                "num=$num, reserveId=$reserveId)"
    }

    companion object {
        private val log = KotlinLogging.logger {  }
    }
}

data class Credit(var creditLimit: BigDecimal)