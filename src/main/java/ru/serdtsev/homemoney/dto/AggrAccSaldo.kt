package ru.serdtsev.homemoney.dto

import java.math.BigDecimal

data class AggrAccSaldo(var type: Account.Type?, var saldo: BigDecimal?) {
  constructor(): this(null, null)
}
