package ru.serdtsev.homemoney.dto

import java.sql.Date
import java.time.LocalDate
import java.util.*
import javax.xml.bind.annotation.XmlElement

open class Account {
  enum class Type {
    debit, credit, expense, income, reserve, asset, service
  }

  var id: UUID? = null
  var name: String? = null
  var type: Type? = null

  private var _createdDate: Date? = null
  var createdDate: Date
    get() = Optional.ofNullable<Date>(this._createdDate).orElse(Date.valueOf(LocalDate.now()))
    set(value) {
      this._createdDate = value
    }

  @XmlElement(name = "isArc")
  var isArc: Boolean = false

  constructor() {
  }

  constructor(category: Category) : this(category.id, category.type, category.name) {
  }

  constructor(id: UUID?, type: Type?, name: String?) {
    this.id = id
    this.name = name
    this.type = type
  }

//  @XmlTransient
  fun isBalance() = Type.debit == type
      || Type.credit == type
      || Type.reserve == type

//  override fun equals(o: Any?): Boolean {
//    if (this === o) return true
//    if (o !is Account) return false
//    return id == o.id
//  }
//
//  override fun hashCode(): Int {
//    return if (Optional.ofNullable<UUID>(id).isPresent) id!!.hashCode() else 0
//  }
}
