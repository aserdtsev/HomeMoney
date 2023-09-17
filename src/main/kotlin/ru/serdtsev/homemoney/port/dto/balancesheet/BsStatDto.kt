package ru.serdtsev.homemoney.port.dto.balancesheet

import java.math.BigDecimal
import java.time.LocalDate

data class BsStatDto(
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val debitSaldo: BigDecimal,
    val creditSaldo: BigDecimal,
    val assetSaldo: BigDecimal,
    val totalSaldo: BigDecimal,
    val reserveSaldo: BigDecimal,
    val freeAmount: BigDecimal,
    val actualDebt: BigDecimal = BigDecimal.ZERO,
    val actualCreditCardDebt: BigDecimal = BigDecimal("0.00"),
    val incomeAmount: BigDecimal = BigDecimal("0.00"),
    val chargesAmount: BigDecimal = BigDecimal("0.00"),
    val categories: List<CategoryStatDto> = mutableListOf(),
    val dayStats: List<BsDayStatDto> = listOf()
)