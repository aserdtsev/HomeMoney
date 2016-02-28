package ru.serdtsev.homemoney.dto

import java.math.BigDecimal
import java.sql.Date

class Turnover(var trnDate: Date? = null, var fromAccType: Account.Type? = null, var toAccType: Account.Type? = null) {
  var amount = BigDecimal.ZERO

  override fun equals(other: Any?): Boolean{
    if (this === other) return true
    if (other?.javaClass != javaClass) return false

    other as Turnover

    if (trnDate != other.trnDate) return false
    if (fromAccType != other.fromAccType) return false
    if (toAccType != other.toAccType) return false

    return true
  }

  override fun hashCode(): Int{
    var result = trnDate?.hashCode() ?: 0
    result += 31 * result + (fromAccType?.hashCode() ?: 0)
    result += 31 * result + (toAccType?.hashCode() ?: 0)
    return result
  }
}
