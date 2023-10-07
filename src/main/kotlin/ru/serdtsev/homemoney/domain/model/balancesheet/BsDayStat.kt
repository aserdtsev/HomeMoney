package ru.serdtsev.homemoney.domain.model.balancesheet

import ru.serdtsev.homemoney.domain.model.account.AccountType
import java.math.BigDecimal
import java.time.LocalDate

data class BsDayStat(
    val localDate: LocalDate,
    /** Сумма дохода за день (см. StatService#fillBsDayStatMap) */
    var incomeAmount: BigDecimal = BigDecimal.ZERO,
    /** Сумма расходов за день (см. StatService#fillBsDayStatMap) */
    var chargeAmount: BigDecimal = BigDecimal.ZERO,
    /** Текущая задолженность */
    var debt: BigDecimal = BigDecimal("0.00"),
    var creditCardDebt: BigDecimal = BigDecimal("0.00"),
    var creditCardDebtDelta: BigDecimal = BigDecimal.ZERO
) {
    /** Ассоциативный массив разницы сумм типов за день (см. StatService#fillBsDayStatMap) */
    private val deltaMap = HashMap<AccountType, BigDecimal>()
    /** Ассоциативный массив сальдо типов счетов на дату объекта */
    private val saldoMap = HashMap<AccountType, BigDecimal>()

    val totalSaldo: BigDecimal
        get() = getSaldo(AccountType.debit).add(getSaldo(AccountType.credit)).add(getSaldo(AccountType.asset))

    val freeAmount: BigDecimal
        get() = getSaldo(AccountType.debit) - reserveSaldo + debt + creditCardDebt

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BsDayStat

        if (localDate != other.localDate) return false

        return true
    }

    override fun hashCode(): Int {
        return localDate.hashCode()
    }

}