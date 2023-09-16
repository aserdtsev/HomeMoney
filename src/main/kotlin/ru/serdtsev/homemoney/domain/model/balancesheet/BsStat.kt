package ru.serdtsev.homemoney.domain.model.balancesheet

import com.fasterxml.jackson.annotation.JsonIgnore
import ru.serdtsev.homemoney.domain.model.account.AccountType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class BsStat(
    val fromDate: LocalDate,
    val toDate: LocalDate,
    var incomeAmount: BigDecimal = BigDecimal("0.00"),
    var chargesAmount: BigDecimal = BigDecimal("0.00"),
    var dayStats: List<BsDayStat> = mutableListOf(),
    var categories: List<CategoryStat> = mutableListOf(),
    var actualDebt: BigDecimal = BigDecimal.ZERO,
    var actualCreditCardDebt: BigDecimal = BigDecimal("0.00")
) {
    @JsonIgnore
    val saldoMap = HashMap<AccountType, BigDecimal>()

    val freeAmount: BigDecimal
        get() = debitSaldo - reserveSaldo - actualCreditCardDebt

    val reserveSaldo: BigDecimal
        get() = saldoMap.getOrDefault(AccountType.reserve, BigDecimal.ZERO)

    val totalSaldo: BigDecimal
        get() = debitSaldo.add(creditSaldo).add(assetSaldo)

    val debitSaldo: BigDecimal
        get() = getSaldo(AccountType.debit)

    val creditSaldo: BigDecimal
        get() = getSaldo(AccountType.credit)

    val assetSaldo: BigDecimal
        get() = getSaldo(AccountType.asset)

    private fun getSaldo(type: AccountType): BigDecimal {
        return saldoMap.getOrDefault(type, BigDecimal.ZERO)
    }
}