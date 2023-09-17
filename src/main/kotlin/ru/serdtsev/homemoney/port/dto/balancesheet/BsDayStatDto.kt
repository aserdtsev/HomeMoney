package ru.serdtsev.homemoney.port.dto.balancesheet

import java.math.BigDecimal

data class BsDayStatDto(
    // Unix-дата и время конца дня в UTC. Так нужно для визуального компонента.
    val date: Long,
    val totalSaldo: BigDecimal,
    val freeAmount: BigDecimal,
    val incomeAmount: BigDecimal = BigDecimal.ZERO,
    val chargeAmount: BigDecimal = BigDecimal.ZERO,
    val debt: BigDecimal = BigDecimal("0.00")
)