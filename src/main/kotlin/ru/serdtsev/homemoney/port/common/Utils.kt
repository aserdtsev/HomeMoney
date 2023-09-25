package ru.serdtsev.homemoney.port.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneOffset

fun localDateToLong(localDate: LocalDate) =
    localDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() - 1

fun moneyScale(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_DOWN)