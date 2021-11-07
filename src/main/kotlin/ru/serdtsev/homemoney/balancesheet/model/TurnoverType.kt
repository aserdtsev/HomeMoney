package ru.serdtsev.homemoney.balancesheet.model

import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.moneyoper.model.CategoryType
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperType

enum class TurnoverType {
    debit, credit, reserve, asset, service, income, expense;

    companion object {
        fun valueOf(accountType: AccountType): TurnoverType = TurnoverType.valueOf(accountType.toString())
        fun valueOf(categoryType: CategoryType): TurnoverType = TurnoverType.valueOf(categoryType.toString())
        fun valueOf(moneyOperType: MoneyOperType): TurnoverType = TurnoverType.valueOf(moneyOperType.toString())
    }
}