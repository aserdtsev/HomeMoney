package ru.serdtsev.homemoney.port.converter.moneyoper

import ru.serdtsev.homemoney.domain.model.account.Reserve
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperType

fun getOperType(oper: MoneyOper) =
    if (oper.type == MoneyOperType.transfer && oper.items.any { it.balance is Reserve }) {
        val operItem = oper.items.first { it.balance is Reserve }
        if (operItem.value.signum() > 0) MoneyOperType.income.name else MoneyOperType.expense.name
    } else {
        oper.type.name
    }
