package ru.serdtsev.homemoney.dto

import java.math.BigDecimal
import java.util.*
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlTransient

open class Balance : Account {
  var reserveId: UUID? = null

  var currencyCode: String? = null
    get() = field ?: "RUB"

  @XmlTransient
  fun getCurrency() = java.util.Currency.getInstance(currencyCode)

  var value: BigDecimal? = null
    get() = field ?: BigDecimal.ZERO.setScale(getCurrency().defaultFractionDigits)

  var creditLimit: BigDecimal? = null
    get() = (field ?: BigDecimal.ZERO).setScale(getCurrency().defaultFractionDigits)

  var minValue: BigDecimal? = null
    get() = field ?: BigDecimal.ZERO

  var num: Long? = null
    get() = field ?: 0L

  constructor(id: UUID, type: Account.Type, name: String, currencyCode: String, value: BigDecimal, reserveId: UUID? = null,
      creditLimit: BigDecimal? = null, minValue: BigDecimal? = null) : super(id, type, name) {
    this.currencyCode = currencyCode
    this.value = value
    this.reserveId = reserveId
    this.creditLimit = creditLimit
    this.minValue = minValue
  }

  constructor() : super()

  @Suppress("unused")
  @XmlElement(name = "freeFunds")
  fun getFreeFunds() = value!!.add(creditLimit!!.subtract(minValue))

  // Для сериализации класса из JSON.
  @Suppress("unused", "unused_parameter")
  @XmlElement(name = "freeFunds")
  fun setFreeFunds(value: BigDecimal) {
  }
}
