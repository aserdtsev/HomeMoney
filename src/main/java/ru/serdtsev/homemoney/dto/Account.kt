package ru.serdtsev.homemoney.dto

import java.sql.Date
import java.time.LocalDate
import java.util.*
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlTransient

open class Account(var id: UUID?, var type: Type?, var name: String?) {
  enum class Type {
    debit, credit, expense, income, reserve, asset, service
  }

  var createdDate: Date = Date.valueOf(LocalDate.now())

  @XmlElement(name = "isArc")
  var arc: Boolean = false
  fun isArc() = arc

  constructor(): this(null, null, null)
  constructor(category: Category) : this(category.id, category.type, category.name)

  @XmlTransient
  fun isBalance() = Type.debit == type || Type.credit == type || Type.reserve == type

  override fun equals(other: Any?): Boolean {
    return (this === other) || (other is Account && id == other.id)
  }

  override fun hashCode(): Int {
    return id?.hashCode() ?: 0
  }
}
