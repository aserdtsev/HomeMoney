package ru.serdtsev.homemoney.account.model

import com.fasterxml.jackson.annotation.JsonIgnore
import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import ru.serdtsev.homemoney.account.ReserveRepository
import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.MoneyOperService
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import ru.serdtsev.homemoney.moneyoper.model.Period
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Date
import java.time.LocalDate
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "balances")
@DiscriminatorValue("balance")
open class Balance(
        id: UUID,
        balanceSheet: BalanceSheet?,
        type: AccountType,
        name: String,
        createdDate: Date,
        isArc: Boolean? = null,
        currencyCode: String?,
        value: BigDecimal?
) : Account(id, balanceSheet, type, name, createdDate, isArc) {
    @Column(name = "currency_code")
    open var currencyCode: String? = currencyCode
        get() = field ?: "RUB"

    open var value: BigDecimal? = value
        get() = field ?: BigDecimal.ZERO.setScale(currency.defaultFractionDigits, RoundingMode.UP)

    @Column(name = "min_value")
    open var minValue: BigDecimal? = null
        get() = field ?: BigDecimal.ZERO.setScale(currency.defaultFractionDigits, RoundingMode.UP)

    @get:JsonIgnore
    @OneToOne
    @JoinColumn(name = "reserve_id")
    open var reserve: Reserve? = null

    @Column(name = "credit_limit")
    open var creditLimit: BigDecimal? = null
        get() = field ?: BigDecimal.ZERO.setScale(currency.defaultFractionDigits, RoundingMode.UP)

    open var num: Long? = null
        get() = field ?: 0L

    @Transient
    open var reserveId: UUID? = null
        get() = reserve?.id

    fun init(reserveRepo: ReserveRepository?) {
        value = value ?: BigDecimal.ZERO
        creditLimit = creditLimit ?: BigDecimal.ZERO
        minValue = minValue ?: BigDecimal.ZERO
        num = num ?: 0L
        reserve = reserveId?.let { reserveRepo!!.findByIdOrNull(it) }
    }

    fun merge(balance: Balance, reserveRepo: ReserveRepository, moneyOperService: MoneyOperService) {
        super.merge(balance)
        creditLimit = balance.creditLimit
        minValue = balance.minValue
        reserve = balance.reserveId?.let { reserveRepo.findByIdOrNull(it) }
        if (balance.value!!.compareTo(value) != 0) {
            val balanceSheet = balanceSheet!!
            val more = balance.value!!.compareTo(value) > 0
            val fromAccId = if (more) balanceSheet.uncatIncome!!.id else balance.id
            val toAccId = if (more) balance.id else balanceSheet.uncatCosts!!.id
            val amount = balance.value!!.subtract(value).abs()
            if (balance.type == AccountType.reserve) {
                value = balance.value
            } else {
                val moneyOper = moneyOperService.newMoneyOper(balanceSheet, UUID.randomUUID(),
                        MoneyOperStatus.pending, LocalDate.now(),0, ArrayList(), "корректировка остатка",
                        Period.single, fromAccId, toAccId, amount, amount)
                moneyOper.complete()
                moneyOperService.save(moneyOper)
            }
        }
    }

    open val currencySymbol: String
        get() = currency.symbol

    open val currency: Currency
        get() {
            assert(currencyCode != null) { this.toString() }
            return Currency.getInstance(currencyCode)
        }

    @Deprecated("")
    fun changeValue(amount: BigDecimal?, trnId: UUID, status: MoneyOperStatus) {
        val beforeValue = value!!.plus()
        value = value!!.add(amount)
        log.info("Balance value changed; " +
                "id: " + id + ", " +
                "trnId: " + trnId + ", " +
                "status: " + status.name + ", " +
                "before: " + beforeValue + ", " +
                "after: " + value + ".")
    }

    fun changeValue(amount: BigDecimal?, oper: MoneyOper) {
        val beforeValue = value!!.plus()
        value = value!!.add(amount)
        log.info("Balance value changed; " +
                "id: " + id + ", " +
                "operId: " + oper.id + ", " +
                "status: " + oper.status!!.name + ", " +
                "before: " + beforeValue + ", " +
                "after: " + value + ".")
    }

    open val freeFunds: BigDecimal
        get() = value!!.add(creditLimit!!.subtract(minValue))

    companion object {
        private val log = KotlinLogging.logger {  }
    }
}