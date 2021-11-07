package ru.serdtsev.homemoney.moneyoper.dto

import ru.serdtsev.homemoney.moneyoper.model.Period
import java.time.LocalDate
import java.util.*

class RecurrenceOperDto(
    val id: UUID,
    val sampleId: UUID,
    val lastMoneyTrnId: UUID,
    val nextDate: LocalDate,
    val period: Period,
    val comment: String?,
    val tags: List<String>,
    val type: String
) {
    var status: Status? = null
    var items: List<MoneyOperItemDto> = mutableListOf()

    enum class Status { active, deleted }
}