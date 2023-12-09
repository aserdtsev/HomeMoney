package ru.serdtsev.homemoney.domain.model.moneyoper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

internal class MonthRecurrenceParamsTest {

    @ParameterizedTest
    @MethodSource("params for getNext")
    fun getNext(dayOfMonth: Int, date: LocalDate, expected: LocalDate) {
        assertThat(MonthRecurrenceParams(dayOfMonth).getNext(date))
            .isEqualTo(expected)
    }

    @Test
    internal fun `getNext by series`() {
        val recurrenceParams = MonthRecurrenceParams(1)

        val next = with (recurrenceParams) {
            getNext(getNext(LocalDate.parse("2024-01-01")))
        }

        assertThat(next).isEqualTo(LocalDate.parse("2024-03-01"))
    }


    companion object {
        @JvmStatic
        fun `params for getNext`() = listOf(
            arguments(1,
                LocalDate.parse("2023-01-01"),
                LocalDate.parse("2023-02-01")),
            arguments(1,
                LocalDate.parse("2023-01-31"),
                LocalDate.parse("2023-02-01")),
            arguments(7,
                LocalDate.parse("2023-11-13"),
                LocalDate.parse("2023-12-07")),
            arguments(7,
                LocalDate.parse("2023-12-13"),
                LocalDate.parse("2024-01-07")),
            arguments(29,
                LocalDate.parse("2024-02-01"),
                LocalDate.parse("2024-02-29")),
            arguments(29,
                LocalDate.parse("2023-02-01"),
                LocalDate.parse("2023-02-28")),
            arguments(-1,
                LocalDate.parse("2024-02-01"),
                LocalDate.parse("2024-02-29")),
            arguments(-28,
                LocalDate.parse("2024-02-01"),
                LocalDate.parse("2024-02-02")),
            arguments(-29,
                LocalDate.parse("2024-02-02"),
                LocalDate.parse("2024-03-01"))
        )
    }

}