package ru.serdtsev.homemoney.dto

import java.math.BigDecimal
import java.util.*
import javax.xml.bind.annotation.XmlTransient

@Suppress("unused")
class BsDayStat {
  var date: Long = 0L

  @XmlTransient
  fun getDateAsLocalDate() = java.sql.Date(date).toLocalDate()

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

  constructor(date: Long) {
    this.date = date
  }

  @XmlTransient
  fun getSaldo(type: Account.Type) = saldoMap.getOrDefault(type, BigDecimal.ZERO)

  @XmlTransient
  fun setSaldo(type: Account.Type, value: BigDecimal) {
    saldoMap.put(type, value.plus())
  }

  @XmlTransient
  fun getDelta(type: Account.Type) = deltaMap.getOrDefault(type, BigDecimal.ZERO)

  @XmlTransient
  fun setDelta(type: Account.Type, amount: BigDecimal) {
    deltaMap.put(type, amount)
  }

}
