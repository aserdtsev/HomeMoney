package ru.serdtsev.homemoney.domain.model.balancesheet

import ru.serdtsev.homemoney.domain.event.DomainEvent
import java.io.Serializable
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

data class BalanceSheet(
    val id: UUID = UUID.randomUUID(),
    val createdTs: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS),
    val currencyCode: String = "RUB"
) : DomainEvent, Serializable {
    fun getCurrency(): Currency = Currency.getInstance(currencyCode)
    fun getCurrencyFractionDigits(): Int = getCurrency().defaultFractionDigits
}