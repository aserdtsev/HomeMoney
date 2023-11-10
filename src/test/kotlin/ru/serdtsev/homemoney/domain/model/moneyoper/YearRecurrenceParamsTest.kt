package ru.serdtsev.homemoney.domain.model.moneyoper

import org.junit.jupiter.api.Assertions.*
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

    companion object {
        @JvmStatic
        fun `params for getNext`() = listOf(
            arguments(1, 1, "2023-12-31", "2024-01-01"),
            arguments(1, 1, "2023-12-30", "2024-01-01"),
            arguments(1, 1, "2024-01-01", "2025-01-01"),
            arguments(2, 29, "2024-02-28", "2024-02-29"),
            arguments(2, 29, "2023-02-27", "2023-02-28"),
            arguments(2, 29, "2023-02-28", "2024-02-29"),
        )
    }
}