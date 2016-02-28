package ru.serdtsev.homemoney.dto

import java.math.BigDecimal
import java.sql.Date
import java.util.*
import javax.xml.bind.annotation.XmlTransient

@Suppress("unused")
class BsDayStat(var date: Date) {
  @XmlTransient
  private val saldoMap = HashMap<Account.Type, BigDecimal>()
  @XmlTransient
  private val deltaMap = HashMap<Account.Type, BigDecimal>()
  @XmlTransient
  var incomeAmount = BigDecimal.ZERO
  @XmlTransient
  var chargeAmount = BigDecimal.ZERO

  val totalSaldo: BigDecimal
    get() = getSaldo(Account.Type.debit).add(getSaldo(Account.Type.credit)).add(getSaldo(Account.Type.asset))

  val freeAmount: BigDecimal
    get() = getSaldo(Account.Type.debit).subtract(reserveSaldo)

  val reserveSaldo: BigDecimal
    get() = getSaldo(Account.Type.reserve)

  @XmlTransient
  fun getSaldo(type: Account.Type): BigDecimal {
    return (saldoMap as java.util.Map<Account.Type, BigDecimal>).getOrDefault(type, BigDecimal.ZERO)
  }

  @XmlTransient
  fun setSaldo(type: Account.Type, value: BigDecimal) {
    saldoMap.put(type, value.plus())
  }

  @XmlTransient
  fun getDelta(type: Account.Type): BigDecimal {
    return (deltaMap as java.util.Map<Account.Type, BigDecimal>).getOrDefault(type, BigDecimal.ZERO)
  }

  @XmlTransient
  fun setDelta(type: Account.Type, amount: BigDecimal) {
    deltaMap.put(type, amount)
  }

}
