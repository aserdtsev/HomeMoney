package ru.serdtsev.homemoney.domain.model.moneyoper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

internal class YearRecurrenceParamsTest {

    @ParameterizedTest
    @MethodSource("params for getNext")
    fun getNext(month: Int, day: Int, date: String, expected: String) {
        assertEquals(LocalDate.parse(expected),
            YearRecurrenceParams(month, day).getNext(LocalDate.parse(date)))
    }

    @Test
    internal fun `getNext by series`() {
        val recurrenceParams = YearRecurrenceParams(1, 1)

        val next = with(recurrenceParams) {
            getNext(getNext(LocalDate.parse("2024-01-01")))
        }

        assertThat(next).isEqualTo(LocalDate.parse("2026-01-01"))
    }

    companion object {
        @JvmStatic
        fun `params for getNext`() = listOf(
            arguments(1, 1, "2024-01-01", "2025-01-01"),
            arguments(1, 1, "2023-12-31", "2024-01-01"),
            arguments(1, 1, "2024-01-02", "2025-01-01"),
            arguments(2, 29, "2024-02-29", "2025-02-28"),
            arguments(2, 29, "2023-02-28", "2024-02-29"),
            arguments(2, 28, "2024-02-29", "2025-02-28")
        )
    }
}