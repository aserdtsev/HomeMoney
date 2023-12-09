package ru.serdtsev.homemoney.domain.model.moneyoper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.DayOfWeek
import java.time.DayOfWeek.*
import java.time.LocalDate

internal class WeekRecurrenceParamsTest {

    @ParameterizedTest
    @MethodSource("params for getNext")
    fun getNext(daysOfWeek: List<DayOfWeek>, date: LocalDate, expected: LocalDate) {
        assertThat(WeekRecurrenceParams(daysOfWeek).getNext(date))
            .isEqualTo(expected)
    }

    companion object {
        @JvmStatic
        fun `params for getNext`() = listOf(
            arguments(listOf(WEDNESDAY), LocalDate.parse("2023-11-01"), LocalDate.parse("2023-11-08")),
            arguments(listOf(TUESDAY), LocalDate.parse("2023-11-01"), LocalDate.parse("2023-11-07")),
            arguments(listOf(TUESDAY, SUNDAY), LocalDate.parse("2023-11-01"), LocalDate.parse("2023-11-05"))
        )
    }
}