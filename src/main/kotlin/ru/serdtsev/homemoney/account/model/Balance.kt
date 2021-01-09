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
@Table(name = "balance")
@DiscriminatorValue("balance")
open class Balance(
        id: UUID,
        balanceSheet: BalanceSheet?,
        type: AccountType,
        name: String,
        createdDate: Date,
        isArc: Boolean? = null,
        open var value: BigDecimal,
        open var currencyCode: String
) : Account(id, balanceSheet, type, name, createdDate, isArc) {
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
        value = value
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
        if (balance.value.compareTo(value) != 0) {
            val balanceSheet = balanceSheet!!
            val amount = balance.value.subtract(value).abs()
            if (balance.type == AccountType.reserve) {
                value = balance.value
            } else {
                val moneyOper = MoneyOper(balanceSheet, MoneyOperStatus.pending, LocalDate.now(), 0, emptyList(),
                        "корректировка остатка", Period.single)
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

    @Deprecated("")
    fun changeValue(amount: BigDecimal?, trnId: UUID, status: MoneyOperStatus) {
        val beforeValue = value.plus()
        value = value.add(amount)
        log.info("Balance value changed; " +
                "id: " + id + ", " +
                "trnId: " + trnId + ", " +
                "status: " + status.name + ", " +
                "before: " + beforeValue + ", " +
                "after: " + value + ".")
    }

    fun changeValue(amount: BigDecimal?, oper: MoneyOper) {
        val beforeValue = value.plus()
        value = value.add(amount)
        log.info("Balance value changed; " +
                "id: " + id + ", " +
                "operId: " + oper.id + ", " +
                "status: " + oper.status.name + ", " +
                "before: " + beforeValue + ", " +
                "after: " + value + ".")
    }

    open val freeFunds: BigDecimal
        get() = value.add(creditLimit!!.subtract(minValue))

    companion object {
        private val log = KotlinLogging.logger {  }
    }
}