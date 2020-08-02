package ru.serdtsev.homemoney.moneyoper

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository
import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import java.time.LocalDate
import java.util.*
import java.util.stream.Stream

interface MoneyOperRepo : PagingAndSortingRepository<MoneyOper, UUID> {
    fun findByBalanceSheet(balanceSheet: BalanceSheet): List<MoneyOper>
    fun findByBalanceSheetAndStatus(balanceSheet: BalanceSheet, status: MoneyOperStatus, pageable: Pageable): Page<MoneyOper>
    fun findByBalanceSheetAndStatusAndId(balanceSheet: BalanceSheet, status: MoneyOperStatus, id: UUID,
            pageable: Pageable): Page<MoneyOper>
    fun findByBalanceSheetAndStatusAndPerformed(balanceSheet: BalanceSheet, status: MoneyOperStatus, performed: LocalDate,
            pageable: Pageable): Page<MoneyOper>
    fun findByBalanceSheetAndStatusAndPerformed(balanceSheet: BalanceSheet, status: MoneyOperStatus,
            performed: LocalDate): List<MoneyOper>
    fun findByBalanceSheetAndStatusAndPerformedGreaterThan(balanceSheet: BalanceSheet, status: MoneyOperStatus,
            performed: LocalDate): List<MoneyOper>
}