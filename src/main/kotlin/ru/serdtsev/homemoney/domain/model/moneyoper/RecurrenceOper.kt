package ru.serdtsev.homemoney.domain.model.moneyoper

import ru.serdtsev.homemoney.domain.event.DomainEvent
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.repository.RepositoryRegistry
import java.io.Serializable
import java.time.LocalDate
import java.util.*

data class RecurrenceOper(
    val id: UUID,
    /** Id шаблона операции (MoneyOper) */
    val templateId: UUID,
    var nextDate: LocalDate,
    var arc: Boolean = false
) : DomainEvent, Serializable {
    constructor(templateId: UUID, nextDate: LocalDate) : this(UUID.randomUUID(), templateId, nextDate)

    fun skipNextDate(): LocalDate {
        nextDate = calcNextDate(nextDate)
        DomainEventPublisher.instance.publish(this)
        return nextDate
    }

    fun calcNextDate(date: LocalDate): LocalDate =
        when (getTemplate().period) {
            Period.month -> date.plusMonths(1)
            Period.quarter -> date.plusMonths(3)
            Period.year -> date.plusYears(1)
            else -> date
        }

    /**
     * Переводит повторяющуюся операцию в архив.
     */
    fun arc() {
        arc = true
    }

    private fun getTemplate(): MoneyOper {
        return RepositoryRegistry.instance.moneyOperRepository.findById(templateId)
    }

}