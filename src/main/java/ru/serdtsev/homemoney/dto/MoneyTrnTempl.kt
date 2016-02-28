package ru.serdtsev.homemoney.dto

import java.math.BigDecimal
import java.sql.Date
import java.time.LocalDate
import java.util.*
import javax.xml.bind.annotation.XmlTransient

/**
 * Шаблон операции
 */
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
  var comment: String? = null
  var labels: List<String>? = null

  @SuppressWarnings("unused")
  constructor() {
  }

  constructor(id: UUID, sampleId: UUID, lastMoneyTrnId: UUID, nextDate: Date, period: MoneyTrn.Period,
              fromAccId: UUID, toAccId: UUID, amount: BigDecimal, comment: String, labels: List<String>) {
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

  val labelsAsString: String?
    @XmlTransient
    get() = labels?.joinToString(",")

  companion object {
    fun calcNextDate(origDate: Date, period: MoneyTrn.Period): Date {
      val origLocalDate = origDate.toLocalDate()
      var nextDate: LocalDate? = null
      when (period) {
        MoneyTrn.Period.month -> nextDate = origLocalDate.plusMonths(1)
        MoneyTrn.Period.quarter -> nextDate = origLocalDate.plusMonths(3)
        MoneyTrn.Period.year -> nextDate = origLocalDate.plusYears(1)
      }
      return Date.valueOf(nextDate)
    }
  }
}
