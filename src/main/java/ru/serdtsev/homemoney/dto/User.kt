package ru.serdtsev.homemoney.dto

import java.util.*

data class User(var userId: UUID?, var email: String?, var pwdHash: String?, var bsId: UUID?) {
  constructor() : this(null, null, null, null)
}
