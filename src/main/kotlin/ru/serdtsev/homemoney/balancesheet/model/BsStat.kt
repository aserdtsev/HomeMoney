package ru.serdtsev.homemoney.balancesheet.model

import com.fasterxml.jackson.annotation.JsonIgnore
import ru.serdtsev.homemoney.account.model.AccountType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class BsStat(
    val fromDate: LocalDate,
    val toDate: LocalDate
) {
    var incomeAmount: BigDecimal = BigDecimal.ZERO
    var chargesAmount: BigDecimal = BigDecimal.ZERO
    var dayStats: List<BsDayStat>? = null
    var categories: List<CategoryStat>? = null

    @JsonIgnore
    val saldoMap = HashMap<AccountType, BigDecimal>()
    @JsonIgnore
    var actualDebt: BigDecimal = BigDecimal.ZERO;

    val freeAmount: BigDecimal
        get() = debitSaldo - reserveSaldo + actualDebt

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