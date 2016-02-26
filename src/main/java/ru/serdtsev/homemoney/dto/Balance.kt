package ru.serdtsev.homemoney.dto

import java.math.BigDecimal
import java.util.*
import javax.xml.bind.annotation.XmlElement

open class Balance : Account {
  var reserveId: UUID? = null
  var value: BigDecimal = BigDecimal.ZERO

  var creditLimit: BigDecimal? = null
    set(value) {
      this.creditLimit = value ?: BigDecimal.ZERO
    }

  var minValue: BigDecimal? = BigDecimal.ZERO
    set(value) {
      this.minValue = value ?: BigDecimal.ZERO
    }

  var num: Long = 0

  constructor(id: UUID, type: Account.Type, name: String, value: BigDecimal, reserveId: UUID,
              creditLimit: BigDecimal, minValue: BigDecimal) : super(id, type, name) {
    this.value = value
    this.reserveId = reserveId
    this.creditLimit = creditLimit
    this.minValue = minValue
  }

  constructor() : super()

  // Поле вычисляемое. Метод нужен для сериализации класса из JSON.
  var freeFunds: BigDecimal
    @XmlElement(name = "freeFunds")
    get() = value.add(creditLimit?.subtract(minValue))

    @XmlElement(name = "freeFunds")
    set(availableBalance) {
    }
}
