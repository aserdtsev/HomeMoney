package ru.serdtsev.homemoney.port.dto.balancesheet

import ru.serdtsev.homemoney.port.common.longToLocalDate
import ru.serdtsev.homemoney.port.common.moneyScale
import java.math.BigDecimal
import java.time.LocalDate

data class BsDayStatDto(
    // Unix-дата и время конца дня в UTC. Так нужно для визуального компонента.
    val date: Long,
    val totalSaldo: BigDecimal = moneyScale(BigDecimal.ZERO),
    val freeAmount: BigDecimal = moneyScale(BigDecimal.ZERO),
    val incomeAmount: BigDecimal = moneyScale(BigDecimal.ZERO),
    val chargeAmount: BigDecimal = moneyScale(BigDecimal.ZERO),
    val debt: BigDecimal = BigDecimal("0.00")
) {
    override fun toString(): String {
        return "BsDayStatDto(date=${longToLocalDate(date)}, totalSaldo=$totalSaldo, freeAmount=$freeAmount, incomeAmount=$incomeAmount, chargeAmount=$chargeAmount, debt=$debt)"
    }
}