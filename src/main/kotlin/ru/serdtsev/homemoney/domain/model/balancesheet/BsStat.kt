package ru.serdtsev.homemoney.domain.model.balancesheet

import ru.serdtsev.homemoney.domain.model.account.AccountType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class BsStat(
    val fromDate: LocalDate,
    val toDate: LocalDate,
    var incomeAmount: BigDecimal = BigDecimal.ZERO,
    var chargesAmount: BigDecimal = BigDecimal.ZERO,
    val categories: List<CategoryStat> = listOf(),
    val currentDebt: BigDecimal = BigDecimal.ZERO,
    val currentCreditCardDebt: BigDecimal = BigDecimal.ZERO,
    var dayStats: List<BsDayStat> = listOf()
) {
    val saldoMap = HashMap<AccountType, BigDecimal>()

    val freeAmount: BigDecimal
        get() = debitSaldo - reserveSaldo + currentCreditCardDebt

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