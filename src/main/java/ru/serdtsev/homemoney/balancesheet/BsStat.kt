package ru.serdtsev.homemoney.balancesheet

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import ru.serdtsev.homemoney.account.model.AccountType

import java.math.BigDecimal
import java.time.LocalDate
import java.util.HashMap
import java.util.UUID

class BsStat {
    var bsId: UUID? = null
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val fromDate: LocalDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    val toDate: LocalDate
    var incomeAmount: BigDecimal = BigDecimal.ZERO
    var chargesAmount: BigDecimal = BigDecimal.ZERO
    var dayStats: List<BsDayStat>? = null
    var categories: List<CategoryStat>? = null

    @JsonIgnore
    val saldoMap = HashMap<AccountType, BigDecimal>()

    val freeAmount: BigDecimal
        get() = debitSaldo.subtract(reserveSaldo)

    private val reserveSaldo: BigDecimal
        get() = saldoMap.getOrDefault(AccountType.reserve, BigDecimal.ZERO)

    val totalSaldo: BigDecimal
        get() = debitSaldo.add(creditSaldo).add(assetSaldo)

    private val debitSaldo: BigDecimal
        get() = getSaldo(AccountType.debit)

    private val creditSaldo: BigDecimal
        get() = getSaldo(AccountType.credit)

    private val assetSaldo: BigDecimal
        get() = getSaldo(AccountType.asset)

    constructor(bsId: UUID, fromDate: LocalDate, toDate: LocalDate) {
        this.bsId = bsId
        this.fromDate = fromDate
        this.toDate = toDate
    }

    private fun getSaldo(type: AccountType): BigDecimal {
        return saldoMap.getOrDefault(type, BigDecimal.ZERO)
    }
}
