package ru.serdtsev.homemoney.dto

import java.math.BigDecimal

class Reserve : Balance {
  var target: BigDecimal?
    get() = field ?: BigDecimal.ZERO
  constructor(): this(null)
  constructor(target: BigDecimal?) {
    this.target = target
  }
}
