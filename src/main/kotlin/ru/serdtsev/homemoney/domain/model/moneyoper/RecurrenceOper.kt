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
    var nextDate: LocalDate,
    var arc: Boolean = false
) : DomainEvent, Serializable {
    init {
        template.linkToRecurrenceOper(this)
    }

    fun createNextMoneyOper(): MoneyOper {
        val moneyOper = MoneyOper(MoneyOperStatus.Recurrence,
            nextDate,
            template.tags,
            template.comment,
            template.period,
            dateNum = 0)
        template.items.forEach { moneyOper.addItem(it.balance, it.value, nextDate) }
        moneyOper.linkToRecurrenceOper(this)
        return moneyOper
    }

    fun skipNextDate(): LocalDate {
        nextDate = calcNextDate(nextDate)
        DomainEventPublisher.instance.publish(this)
        return nextDate
    }

    /**
     * Возвращает следующюю за {@param date} дату повтора
     */
    fun calcNextDate(date: LocalDate): LocalDate {
        val recurrenceParams = template.recurrenceParams
            ?: when (template.period) {
                Period.Day -> DayRecurrenceParams(1)
                Period.Week -> WeekRecurrenceParams(listOf(nextDate.dayOfWeek))
                Period.Month -> MonthRecurrenceParams(nextDate.dayOfMonth)
                Period.Year -> YearRecurrenceParams(nextDate.monthValue, nextDate.dayOfMonth)
                else -> throw IllegalStateException()
            }
        return recurrenceParams.getNext(date)
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
            val recurrenceOper = RecurrenceOper(UUID.randomUUID(), template, template.performed).apply {
                nextDate = calcNextDate(nextDate)
            }
            DomainEventPublisher.instance.publish(recurrenceOper)

            sample.linkToRecurrenceOper(recurrenceOper)
            DomainEventPublisher.instance.publish(sample)

            return recurrenceOper
        }
    }

}