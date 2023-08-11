package ru.serdtsev.homemoney.port.dto.moneyoper

import ru.serdtsev.homemoney.domain.model.moneyoper.Period
import java.time.LocalDate
import java.util.*

class RecurrenceOperDto(
    val id: UUID,
    var nextDate: LocalDate,
    var period: Period,
    var comment: String?,
    var tags: List<String>,
    val type: String
) {
    var status: Status? = null
    var items: List<MoneyOperItemDto> = mutableListOf()

    enum class Status { active, deleted }
}