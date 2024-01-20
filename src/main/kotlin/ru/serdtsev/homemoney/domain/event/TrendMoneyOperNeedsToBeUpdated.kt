package ru.serdtsev.homemoney.domain.event

import ru.serdtsev.homemoney.domain.model.moneyoper.Period
import ru.serdtsev.homemoney.domain.model.moneyoper.Tag
import java.time.LocalDate

data class TrendMoneyOperNeedsToBeUpdated(
    val period: Period,
    val category: Tag,
    val calculatedDate: LocalDate
): DomainEvent