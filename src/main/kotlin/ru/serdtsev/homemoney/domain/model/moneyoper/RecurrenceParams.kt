package ru.serdtsev.homemoney.domain.model.moneyoper

import java.time.DayOfWeek

interface RecurrenceParams
data class DayRecurrenceParams(val n: Int) : RecurrenceParams
data class WeekRecurrenceParams(val daysOfWeek: List<DayOfWeek>) : RecurrenceParams
data class MonthRecurrenceParams(val dayOfMonth: Int) : RecurrenceParams
data class YearRecurrenceParams(val month: Int, val day: Int) : RecurrenceParams

fun getRecurrenceParamsClass(period: Period) =
    when (period) {
        Period.Day -> DayRecurrenceParams::class.java
        Period.Week -> WeekRecurrenceParams::class.java
        Period.Month -> MonthRecurrenceParams::class.java
        Period.Year -> YearRecurrenceParams::class.java
        else -> throw IllegalStateException("Invalid period: $period")
    }