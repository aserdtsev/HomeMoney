package ru.serdtsev.homemoney.moneyoper.model

import ru.serdtsev.homemoney.common.HmException
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class MoneyOperDto(
        val id: UUID,
        val status: MoneyOperStatus?,
        val operDate: LocalDate?,
        val fromAccId: UUID?,
        val toAccId: UUID?,
        val amount: BigDecimal,
        var currencyCode: String?,
        val toAmount: BigDecimal?,
        val toCurrencyCode: String?,
        val period: Period?,
        val comment: String?,
        val labels: List<String>?,
        val dateNum: Int?,
        val parentId: UUID?,
        val recurrenceId: UUID?,
        val createdTs: Timestamp = Timestamp.valueOf(LocalDateTime.now()))
{
    var items: List<MoneyOperItemDto> = ArrayList()
    var fromAccName: String? = null
    var toAccName: String? = null
    var type: String? = null
    @Suppress("unused")
    fun getCurrencySymbol() = Currency.getInstance(currencyCode).symbol
    @Suppress("unused")
    fun getToCurrencySymbol() = Currency.getInstance(toCurrencyCode).symbol
    @Suppress("unused")
    fun isMonoCurrencies() = currencyCode == toCurrencyCode

    init {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw HmException(HmException.Code.WrongAmount)
        }
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MoneyOperDto
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}