package ru.serdtsev.homemoney.domain.event

import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus

data class MoneyOperStatusChanged(
    val beforeStatus: MoneyOperStatus,
    val afterStatus: MoneyOperStatus,
    val moneyOper: MoneyOper
): DomainEvent