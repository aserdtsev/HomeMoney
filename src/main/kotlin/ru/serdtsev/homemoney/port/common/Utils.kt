package ru.serdtsev.homemoney.port.common

import java.time.LocalDate
import java.time.ZoneOffset

fun localDateToLong(localDate: LocalDate) =
    localDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() - 1