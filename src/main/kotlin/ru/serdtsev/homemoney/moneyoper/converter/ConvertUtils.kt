package ru.serdtsev.homemoney.moneyoper.converter

import ru.serdtsev.homemoney.account.model.Reserve
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperType

fun getOperType(oper: MoneyOper) =
    if (oper.type == MoneyOperType.transfer && oper.items.any { it.balance is Reserve }) {
        val operItem = oper.items.first { it.balance is Reserve }
        if (operItem.value.signum() > 0) MoneyOperType.income.name else MoneyOperType.expense.name
    } else {
        oper.type.name
    }
