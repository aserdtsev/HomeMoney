package ru.serdtsev.homemoney.common

class HmException(val code: Code, message: String? = null) : RuntimeException(message) {
    override fun toString(): String {
        return "HmException{" +
                "code=" + code +
                "} " + super.toString()
    }

    enum class Code {
        WrongAuth, UnknownAccountTypeCode, UnknownMoneyTrnStatus, IdentifiersDoNotMatch, WrongAmount, WrongUserId,
        UserIdCookieIsEmpty, BalanceSheetNotFound, BalanceSheetIdNotFound
    }
}