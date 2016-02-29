package ru.serdtsev.homemoney.dto

import java.math.BigDecimal
import java.sql.Date
import java.util.*
import javax.xml.bind.annotation.XmlTransient

@Suppress("unused")
class BsStat(val bsId: UUID, private val fromDate: Date, private val toDate: Date) {
  var incomeAmount = BigDecimal.ZERO
  var chargesAmount = BigDecimal.ZERO
  var dayStats: List<BsDayStat>? = null

  @XmlTransient
  val saldoMap = HashMap<Account.Type, BigDecimal>()

  val debitSaldo: BigDecimal
    get() = saldoMap.getOrDefault(Account.Type.debit, BigDecimal.ZERO)
  val creditSaldo: BigDecimal
    get() = saldoMap.getOrDefault(Account.Type.credit, BigDecimal.ZERO)
  val assetSaldo: BigDecimal
    get() = saldoMap.getOrDefault(Account.Type.asset, BigDecimal.ZERO)
  val reserveSaldo: BigDecimal
    get() = saldoMap.getOrDefault(Account.Type.reserve, BigDecimal.ZERO)
  val totalSaldo: BigDecimal
    get() = debitSaldo.add(creditSaldo).add(assetSaldo)
}
