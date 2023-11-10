package ru.serdtsev.homemoney.domain.model.moneyoper

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.absoluteValue

/**
 * Параметры повторения
 */
interface RecurrenceParams {
    /**
     * Возвращает дату следующего повтора после даты, заданной параметром date
     */
    fun getNext(date: LocalDate): LocalDate
}

/**
 * Повторение для {@link ru.serdtsev.homemoney.domain.model.moneyoper.Period.Day}
 */
data class DayRecurrenceParams(val n: Int?) : RecurrenceParams {
    override fun getNext(date: LocalDate): LocalDate {
        return date.plusDays((n ?: 0).toLong() + 1)
    }
}

/**
 * Повторение для {@link ru.serdtsev.homemoney.domain.model.moneyoper.Period.Week}
 */
data class WeekRecurrenceParams(val daysOfWeek: List<DayOfWeek>) : RecurrenceParams {
    override fun getNext(date: LocalDate): LocalDate {
        assert(daysOfWeek.isNotEmpty())
        var next = date.plusDays(1)
        while (!daysOfWeek.contains(next.dayOfWeek)) {
            next = next.plusDays(1)
        }
        return next
    }
}

/**
 * Повторение для {@link ru.serdtsev.homemoney.domain.model.moneyoper.Period.Month}
 */
data class MonthRecurrenceParams(val dayOfMonth: Int) : RecurrenceParams {
    init {
        assert(dayOfMonth != 0 && abs(dayOfMonth) < 32) { dayOfMonth }
    }
    override fun getNext(date: LocalDate): LocalDate {
        val next = date.plusDays(1)
        val normDayOfMonth = getNormDayOfMonth(next).toLong()
        return if (normDayOfMonth > 0) {
            next.plusDays(normDayOfMonth - next.dayOfMonth)
        } else {
            val nextYearMonth = YearMonth.of(next.year, next.monthValue + 1)
            val d = nextYearMonth.atDay(1).plusDays(normDayOfMonth)
            if (d.isBefore(next)) d.plusMonths(1) else d
        }
    }

    private fun getNormDayOfMonth(date: LocalDate): Int {
        val yearMonth = YearMonth.of(date.year, date.monthValue)
        return if (dayOfMonth > 0) {
            dayOfMonth.coerceAtMost(yearMonth.lengthOfMonth())
        } else {
            -dayOfMonth.absoluteValue.coerceAtMost(yearMonth.lengthOfMonth())
        }
    }
}

/**
 * Повторение для {@link ru.serdtsev.homemoney.domain.model.moneyoper.Period.Year}
 */
data class YearRecurrenceParams(val month: Int, val day: Int) : RecurrenceParams {
    init {
        assert(month in 1..12) { month }
        assert(day in 1..31)
    }

    override fun getNext(date: LocalDate): LocalDate {
        return date.plusDays(1)
            .let { nextDate ->
                val day = this.day.coerceAtMost(YearMonth.of(date.year, month).lengthOfMonth())
                LocalDate.of(date.year, month, day).let { resultCandidate ->
                    if (resultCandidate < nextDate) {
                        val nextYear = date.year + 1
                        this.day.coerceAtMost(YearMonth.of(nextYear, month).lengthOfMonth())
                            .let { day -> LocalDate.of(nextYear, month, day) }
                    } else {
                        resultCandidate
                    }
                }
            }
    }
}

fun getRecurrenceParamsClass(period: Period) =
    when (period) {
        Period.Day -> DayRecurrenceParams::class.java
        Period.Week -> WeekRecurrenceParams::class.java
        Period.Month -> MonthRecurrenceParams::class.java
        Period.Year -> YearRecurrenceParams::class.java
        else -> throw IllegalStateException("Invalid period: $period")
    }