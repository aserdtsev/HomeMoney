package ru.serdtsev.homemoney.balancesheet

import ru.serdtsev.homemoney.account.model.AccountType
import java.math.BigDecimal

data class AggrAccountSaldo (
    var type: AccountType,
    var saldo: BigDecimal
)
