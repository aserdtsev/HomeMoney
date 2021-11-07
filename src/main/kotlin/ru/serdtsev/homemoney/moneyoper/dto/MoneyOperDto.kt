package ru.serdtsev.homemoney.moneyoper.dto

import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import ru.serdtsev.homemoney.moneyoper.model.Period
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class MoneyOperDto(
    val id: UUID,
    val status: MoneyOperStatus,
    val operDate: LocalDate,
    val period: Period?,
    val comment: String?,
    val tags: List<String>,
    val dateNum: Int?,
    val parentId: UUID?,
    val recurrenceId: UUID?,
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