package ru.serdtsev.homemoney.domain.model.balancesheet

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import ru.serdtsev.homemoney.domain.model.account.AccountType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.HashMap

data class BsDayStat(@JsonIgnore val localDate: LocalDate) {
    /** Сумма дохода за день (см. StatService#fillBsDayStatMap) */
    var incomeAmount: BigDecimal = BigDecimal.ZERO
    /** Сумма расходов за день (см. StatService#fillBsDayStatMap) */
    var chargeAmount: BigDecimal = BigDecimal.ZERO
    /** Ассоциативный массив разницы сумм типов за день (см. StatService#fillBsDayStatMap) */
    private val deltaMap = HashMap<AccountType, BigDecimal>()
    /** Ассоциативный массив сальдо типов счетов на дату объекта */
    private val saldoMap = HashMap<AccountType, BigDecimal>()
    var actualDebt: BigDecimal = BigDecimal.ZERO

    // Unix-дата и время конца дня в UTC. Так нужно для визуального компонента.
    val date: Long
        @JsonProperty("date")
        get() = localDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() - 1

    val totalSaldo: BigDecimal
        get() = getSaldo(AccountType.debit).add(getSaldo(AccountType.credit)).add(getSaldo(AccountType.asset))

    val freeAmount: BigDecimal
        get() = getSaldo(AccountType.debit) - reserveSaldo + actualDebt

    private val reserveSaldo: BigDecimal
        get() = getSaldo(AccountType.reserve)

    private val creditSaldo: BigDecimal
        get() = getSaldo(AccountType.credit)

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