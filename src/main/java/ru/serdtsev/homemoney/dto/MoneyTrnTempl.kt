package ru.serdtsev.homemoney.dto

import java.math.BigDecimal
import java.sql.Date
import java.util.*
import javax.xml.bind.annotation.XmlTransient

/**
 * Шаблон операции
 */
@Suppress("unused")
class MoneyTrnTempl {

  enum class Status {
    active, deleted
  }

  var id: UUID? = null
  var status: Status? = null
    get() = if (field != null) field else Status.active
  var sampleId: UUID? = null
  var lastMoneyTrnId: UUID? = null
  var nextDate: Date? = null
  var period: MoneyTrn.Period? = null
  var fromAccId: UUID? = null
  var fromAccName: String? = null
  var toAccId: UUID? = null
  var toAccName: String? = null
  var type: String? = null
  var amount: BigDecimal? = null
  var currencyCode: String? = null
  var currencySymbol: String?
    get() = Currency.getInstance(currencyCode).symbol
    set(value) {}
  var toAmount: BigDecimal? = null
  var toCurrencyCode: String? = null
  var toCurrencySymbol: String?
    get() = Currency.getInstance(toCurrencyCode).symbol
    set(value) {}
  var comment: String? = null
  var labels: List<String>? = null

  constructor() {
  }

  constructor(id: UUID, sampleId: UUID, lastMoneyTrnId: UUID, nextDate: Date, period: MoneyTrn.Period,
              fromAccId: UUID, toAccId: UUID, amount: BigDecimal, comment: String?, labels: List<String>?) {
    this.id = id
    this.sampleId = sampleId
    this.lastMoneyTrnId = lastMoneyTrnId
    this.nextDate = nextDate
    this.period = period
    this.fromAccId = fromAccId
    this.toAccId = toAccId
    this.amount = amount
    this.comment = comment
    this.labels = labels
  }

  @XmlTransient
  fun getLabelsAsString() = labels?.joinToString(",")

  companion object {
    fun calcNextDate(origDate: Date, period: MoneyTrn.Period): Date {
      val origLocalDate = origDate.toLocalDate()
      val nextDate = when (period) {
        MoneyTrn.Period.month -> origLocalDate.plusMonths(1)
        MoneyTrn.Period.quarter -> origLocalDate.plusMonths(3)
        MoneyTrn.Period.year -> origLocalDate.plusYears(1)
        else -> origDate.toLocalDate()
      }
      return Date.valueOf(nextDate)
    }
  }
}
