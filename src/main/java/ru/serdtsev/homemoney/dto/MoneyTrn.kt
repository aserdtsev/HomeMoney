package ru.serdtsev.homemoney.dto

import ru.serdtsev.homemoney.HmException
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import javax.xml.bind.annotation.XmlTransient

@Suppress("unused")
class MoneyTrn {
  enum class Status {
    pending, done, cancelled
  }

  enum class Period {
    month, quarter, year, single
  }

  var id: UUID? = null
  var status: Status? = null
  var trnDate: Date? = null
  var dateNum: Int? = null
  var fromAccId: UUID? = null
  var fromAccName: String? = null
  var toAccId: UUID? = null
  var toAccName: String? = null
  var type: String? = null
  var parentId: UUID? = null
  var amount: BigDecimal? = null
  var currencyCode: String? = null
  val currencySymbol: String?
    get() = Currency.getInstance(currencyCode).symbol
  var toAmount: BigDecimal? = null
  var toCurrencyCode: String? = null
  val toCurrencySymbol: String?
    get() = Currency.getInstance(toCurrencyCode).symbol
  var comment: String? = null
  var createdTs: Timestamp? = null
  var period: Period? = null
    get() = field ?: Period.month
  var labels: List<String>? = null
    get() = field ?: ArrayList(0)
  var templId: UUID? = null

  constructor() {
  }

  constructor(id: UUID, status: Status, trnDate: Date, fromAccId: UUID, toAccId: UUID, amount: BigDecimal, period: Period,
      comment: String? = null, labels: List<String>? = null, dateNum: Int? = 0,
      parentId: UUID? = null, templId: UUID? = null, createdTs: Timestamp? = Timestamp.valueOf(LocalDateTime.now())) {
    if (amount.compareTo(BigDecimal.ZERO) == 0) {
      throw HmException(HmException.Code.AmountWrong)
    }
    this.id = id
    this.status = status
    this.trnDate = trnDate
    this.dateNum = dateNum
    this.fromAccId = fromAccId
    this.toAccId = toAccId
    this.parentId = parentId
    this.amount = amount
    this.comment = comment
    this.createdTs = createdTs
    this.period = period
    this.labels = labels
    this.templId = templId
  }

  val labelsAsString: String?
    @XmlTransient
    get() = labels?.joinToString(",")

  fun crucialEquals(other: MoneyTrn): Boolean {
    if (id != other.id) {
      throw HmException(HmException.Code.IdentifiersDoNotMatch)
    }
    return trnDate == other.trnDate
        && fromAccId == other.fromAccId && toAccId == other.toAccId
        && amount!!.compareTo(other.amount) == 0
        && toAmount!!.compareTo(other.toAmount) == 0
        && status == other.status
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is MoneyTrn) return false
    return !if (id != null) id != other.id else other.id != null
  }

  override fun hashCode(): Int {
    return if (id != null) id!!.hashCode() else 0
  }
}
