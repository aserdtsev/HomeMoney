package ru.serdtsev.homemoney.domain.model.moneyoper

import java.time.DayOfWeek
import java.time.MonthDay

enum class Period { Day, Week, Month, Year, Single }

interface PeriodParams
data class DayPeriodParams(val n: Int) : PeriodParams
data class WeekPeriodParams(val daysOfWeek: List<DayOfWeek>) : PeriodParams
data class MonthPeriodParams(val dayOfMonth: Int) : PeriodParams
data class YearPeriodParams(val monthDay: MonthDay) : PeriodParams