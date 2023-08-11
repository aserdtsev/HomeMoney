package ru.serdtsev.homemoney.domain.model.moneyoper

import mu.KotlinLogging
import ru.serdtsev.homemoney.domain.event.DomainEvent
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import java.io.Serializable
import java.time.LocalDate
import java.util.*

data class RecurrenceOper(
    val id: UUID,
    var template: MoneyOper,
    var nextDate: LocalDate = LocalDate.MAX,
    var arc: Boolean = false
) : DomainEvent, Serializable {
    init {
        template.linkToRecurrenceOper(this)
    }

    fun createNextMoneyOper(): MoneyOper {
        val moneyOper = MoneyOper(MoneyOperStatus.recurrence, nextDate, 0, template.tags, template.comment,
            template.period)
        template.items.forEach { moneyOper.addItem(it.balance, it.value, nextDate) }
        moneyOper.linkToRecurrenceOper(this)
        return moneyOper
    }

    fun skipNextDate(): LocalDate {
        nextDate = calcNextDate(nextDate)
        DomainEventPublisher.instance.publish(this)
        return nextDate
    }

    fun calcNextDate(date: LocalDate): LocalDate =
        when (template.period) {
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
        DomainEventPublisher.instance.publish(this)
        log.info("RecurrenceOper $id moved to archive.")
    }

    companion object {
        private val log = KotlinLogging.logger {  }
        fun of(sample: MoneyOper): RecurrenceOper {
            val template = sample.createTemplate()
            val recurrenceOper = RecurrenceOper(UUID.randomUUID(), template).apply {
                nextDate = calcNextDate(template.performed)
            }
            DomainEventPublisher.instance.publish(recurrenceOper)

            sample.linkToRecurrenceOper(recurrenceOper)
            DomainEventPublisher.instance.publish(sample)

            return recurrenceOper
        }
    }

}