package ru.serdtsev.homemoney.moneyoper

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperItem
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

interface MoneyOperItemRepo : PagingAndSortingRepository<MoneyOperItem, UUID> {
    fun findByBalanceSheetAndValueOrderByPerformedDesc(balanceSheet: BalanceSheet, absValue: BigDecimal,
            pageable: Pageable): Page<MoneyOperItem>
    fun findByBalanceSheetAndPerformedBetweenAndMoneyOperStatus(balanceSheet: BalanceSheet, startDate: LocalDate,
            finishDate: LocalDate, status: MoneyOperStatus): List<MoneyOperItem>
    fun findByBalance(balance: Balance?): List<MoneyOperItem>
}