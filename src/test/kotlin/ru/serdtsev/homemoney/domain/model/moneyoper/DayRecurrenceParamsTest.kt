package ru.serdtsev.homemoney.domain.model.moneyoper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

internal class DayRecurrenceParamsTest {

    @ParameterizedTest
    @MethodSource("params for getNext")
    fun getNext(n: Int, date: String, expected: String) {
        assertThat(DayRecurrenceParams(n).getNext(LocalDate.parse(date)))
            .isEqualTo(LocalDate.parse(expected))
    }

    companion object {
        @JvmStatic
        fun `params for getNext`() = listOf(
            arguments(1, "2023-11-01", "2023-11-02"),
            arguments(2, "2023-11-01", "2023-11-03"),
            arguments(3, "2023-11-01", "2023-11-04")
        )
    }
}