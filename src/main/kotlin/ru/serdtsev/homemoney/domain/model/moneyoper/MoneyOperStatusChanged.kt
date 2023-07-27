package ru.serdtsev.homemoney.domain.model.moneyoper

import ru.serdtsev.homemoney.domain.event.DomainEvent

data class MoneyOperStatusChanged(val beforeStatus: MoneyOperStatus, val afterStatus: MoneyOperStatus,
                                  val moneyOper: MoneyOper
): DomainEvent