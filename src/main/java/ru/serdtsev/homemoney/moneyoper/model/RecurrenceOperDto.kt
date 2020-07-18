package ru.serdtsev.homemoney.moneyoper.model

import com.fasterxml.jackson.annotation.JsonFormat
import lombok.Data
import lombok.NoArgsConstructor
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class RecurrenceOperDto(
        val id: UUID,
        val sampleId: UUID,
        val lastMoneyTrnId: UUID,
        val nextDate: LocalDate,
        val period: Period,
        val fromAccId: UUID,
        val toAccId: UUID,
        val amount: BigDecimal,
        val toAmount: BigDecimal,
        val comment: String,
        val labels: List<String>,
        val currencyCode: String,
        val toCurrencyCode: String,
        val fromAccName: String,
        val toAccName: String,
        val type: String
) {
    var status: Status? = null
    var items: List<MoneyOperItemDto> = ArrayList()

    enum class Status { active, deleted }
}