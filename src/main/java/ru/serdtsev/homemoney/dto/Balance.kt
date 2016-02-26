package ru.serdtsev.homemoney.dto

import java.math.BigDecimal
import java.util.*
import javax.xml.bind.annotation.XmlElement

open class Balance : Account {
  var reserveId: UUID? = null

  var value: BigDecimal? = null
    get() = field ?: BigDecimal.ZERO

  var creditLimit: BigDecimal? = null
    get() = field ?: BigDecimal.ZERO

  var minValue: BigDecimal? = null
    get() = field ?: BigDecimal.ZERO

  var num: Long? = null
    get() = field ?: 0L

  constructor(id: UUID, type: Account.Type, name: String, value: BigDecimal, reserveId: UUID,
              creditLimit: BigDecimal, minValue: BigDecimal) : super(id, type, name) {
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
