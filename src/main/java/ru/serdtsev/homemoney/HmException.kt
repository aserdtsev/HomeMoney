package ru.serdtsev.homemoney

class HmException(private val code: HmException.Code) : RuntimeException() {
  enum class Code {
    WrongAuth,
    UnknownAccountTypeCode,
    UnknownMoneyTrnStatus,
    IdentifiersDoNotMatch,
    WrongAmount,
    UserIdCookieIsEmpty
  }

  fun getCode(): String {
    return code.name
  }
}
