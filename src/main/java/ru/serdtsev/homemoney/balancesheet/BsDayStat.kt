package ru.serdtsev.homemoney.balancesheet

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import ru.serdtsev.homemoney.account.model.AccountType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

class BsDayStat {
    @JsonIgnore
    var localDate: LocalDate? = null
    var incomeAmount: BigDecimal = BigDecimal.ZERO
    var chargeAmount: BigDecimal = BigDecimal.ZERO
    private val saldoMap = HashMap<AccountType, BigDecimal>()
    private val deltaMap = HashMap<AccountType, BigDecimal>()

    val date: Long?
        @JsonProperty("date")
        get() {
            val zoneOffset = OffsetDateTime.now().offset
            return localDate!!.atStartOfDay().toInstant(zoneOffset).toEpochMilli()
        }

    val totalSaldo: BigDecimal
        get() = getSaldo(AccountType.debit).add(getSaldo(AccountType.credit)).add(getSaldo(AccountType.asset))

    val freeAmount: BigDecimal
        get() = getSaldo(AccountType.debit).subtract(reserveSaldo)

    private val reserveSaldo: BigDecimal
        get() = getSaldo(AccountType.reserve)

    constructor(localDate: LocalDate) {
        this.localDate = localDate
    }

    fun setDate(localDate: LocalDate) {
        this.localDate = localDate
    }

    private fun getSaldo(type: AccountType): BigDecimal {
        return saldoMap.getOrDefault(type, BigDecimal.ZERO)
    }

    fun setSaldo(type: AccountType, value: BigDecimal) {
        saldoMap[type] = value.plus()
    }

    fun getDelta(type: AccountType): BigDecimal {
        return deltaMap.getOrDefault(type, BigDecimal.ZERO)
    }

    fun setDelta(type: AccountType, amount: BigDecimal) {
        deltaMap[type] = amount
    }

}
