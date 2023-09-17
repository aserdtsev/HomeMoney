package ru.serdtsev.homemoney.port.dto.balancesheet

import java.math.BigDecimal
import java.time.LocalDate

data class BsStatDto(
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val debitSaldo: BigDecimal = BigDecimal.ZERO,
    val creditSaldo: BigDecimal = BigDecimal.ZERO,
    val assetSaldo: BigDecimal = BigDecimal.ZERO,
    val totalSaldo: BigDecimal = BigDecimal.ZERO,
    val reserveSaldo: BigDecimal = BigDecimal.ZERO,
    val freeAmount: BigDecimal = BigDecimal.ZERO,
    val actualDebt: BigDecimal = BigDecimal.ZERO,
    val actualCreditCardDebt: BigDecimal = BigDecimal("0.00"),
    val incomeAmount: BigDecimal = BigDecimal("0.00"),
    val chargesAmount: BigDecimal = BigDecimal("0.00"),
    val categories: List<CategoryStatDto> = mutableListOf(),
    val dayStats: List<BsDayStatDto> = listOf()
)