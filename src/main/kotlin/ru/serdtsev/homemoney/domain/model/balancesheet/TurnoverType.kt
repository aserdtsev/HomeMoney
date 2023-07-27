package ru.serdtsev.homemoney.domain.model.balancesheet

import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.moneyoper.CategoryType
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperType

enum class TurnoverType {
    debit, credit, reserve, asset, service, income, expense;

    companion object {
        fun valueOf(accountType: AccountType): TurnoverType = TurnoverType.valueOf(accountType.toString())
        fun valueOf(categoryType: CategoryType): TurnoverType = TurnoverType.valueOf(categoryType.toString())
        fun valueOf(moneyOperType: MoneyOperType): TurnoverType = TurnoverType.valueOf(moneyOperType.toString())
    }
}