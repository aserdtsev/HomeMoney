package ru.serdtsev.homemoney.dto

import java.util.*

class Category(var rootId: UUID?) : Account() {
  constructor(): this(null)
}
