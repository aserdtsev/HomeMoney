package ru.serdtsev.homemoney.port.dto.moneyoper

import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import ru.serdtsev.homemoney.domain.model.moneyoper.Period
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class MoneyOperDto(
    val id: UUID,
    val status: MoneyOperStatus,
    val operDate: LocalDate = LocalDate.now(),
    var period: Period? = null,
    var comment: String? = null,
    var tags: List<String> = listOf(),
    val dateNum: Int = 0,
    val parentId: UUID? = null,
    val recurrenceId: UUID? = null,
    val createdTs: Timestamp = Timestamp.valueOf(LocalDateTime.now()))
{
    var items: List<MoneyOperItemDto> = ArrayList()
    var type: String? = null
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MoneyOperDto
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}