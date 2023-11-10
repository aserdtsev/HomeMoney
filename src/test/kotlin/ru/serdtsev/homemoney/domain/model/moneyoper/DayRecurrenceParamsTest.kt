package ru.serdtsev.homemoney.domain.model.moneyoper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import java.time.LocalDate

internal class DayRecurrenceParamsTest {

    @Test
    fun getNext() {
        assertThat(DayRecurrenceParams(0).getNext(LocalDate.parse("2023-11-01")))
            .isEqualTo(LocalDate.parse("2023-11-02"))
        assertThat(DayRecurrenceParams(1).getNext(LocalDate.parse("2023-11-01")))
            .isEqualTo(LocalDate.parse("2023-11-03"))
    }
}