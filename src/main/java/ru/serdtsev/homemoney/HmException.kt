package ru.serdtsev.homemoney

class HmException(private val code: HmException.Code) : RuntimeException() {
  enum class Code {
    AuthWrong,
    UnknownAccountTypeCode,
    UnknownMoneyTrnStatus,
    IdentifiersDoNotMatch,
    AmountWrong
  }

  fun getCode(): String {
    return code.name
  }
}
