package ru.serdtsev.homemoney.port.dto.balancesheet

import ru.serdtsev.homemoney.port.common.moneyScale
import java.math.BigDecimal
import java.time.LocalDate

data class BsStatDto(
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val debitSaldo: BigDecimal = moneyScale(BigDecimal.ZERO),
    val creditSaldo: BigDecimal = moneyScale(BigDecimal.ZERO),
    val assetSaldo: BigDecimal = moneyScale(BigDecimal.ZERO),
    val totalSaldo: BigDecimal = moneyScale(BigDecimal.ZERO),
    val reserveSaldo: BigDecimal = moneyScale(BigDecimal.ZERO),
    val freeAmount: BigDecimal = moneyScale(BigDecimal.ZERO),
    val actualDebt: BigDecimal = moneyScale(BigDecimal.ZERO),
    val actualCreditCardDebt: BigDecimal = moneyScale(BigDecimal.ZERO),
    val incomeAmount: BigDecimal = moneyScale(BigDecimal.ZERO),
    val chargesAmount: BigDecimal = moneyScale(BigDecimal.ZERO),
    val categories: List<CategoryStatDto> = mutableListOf(),
    val dayStats: List<BsDayStatDto> = listOf()
)