package ru.serdtsev.homemoney.domain.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

interface MoneyOperRepository {
    fun findById(id: UUID): MoneyOper
    fun findByIdOrNull(id: UUID): MoneyOper?
    fun exists(id: UUID): Boolean
    fun findByBalanceSheetAndStatus(balanceSheet: BalanceSheet, status: MoneyOperStatus,
         pageable: Pageable): Page<MoneyOper>
    fun findByBalanceSheetAndStatusAndPerformed(balanceSheet: BalanceSheet, status: MoneyOperStatus,
         performed: LocalDate, pageable: Pageable): Page<MoneyOper>
    fun findByBalanceSheetAndStatusAndPerformed(bsId: UUID, status: MoneyOperStatus, performed: LocalDate): List<MoneyOper>
    fun findByBalanceSheetAndStatusAndPerformedGreaterThan(status: MoneyOperStatus, performed: LocalDate): List<MoneyOper>
    fun findByBalanceSheetAndValueOrderByPerformedDesc(balanceSheet: BalanceSheet, absValue: BigDecimal,
        pageable: Pageable): Page<MoneyOper>
    fun findByPerformedBetweenAndMoneyOperStatus(startDate: LocalDate, finishDate: LocalDate,
        status: MoneyOperStatus): List<MoneyOper>
    /** Возвращает расходные операции по кредитным картам, которые не входят в период, но влияют на него */
    fun findByCreditCardChargesThatAffectPeriod(startDate: LocalDate, finishDate: LocalDate): List<MoneyOper>
    fun getCurrentCreditCardDebt(currentDate: LocalDate): BigDecimal
    fun existsByBalance(balance: Balance): Boolean

    /**
     * Возвращает расходные операции по кредитной карте для досрочного гашения задолженности
     */
    fun findByCreditCardChargesForEarlyRepaymentDebt(balanceId: UUID, operDate: LocalDate): List<MoneyOper>
    fun findByCreditCardChargesForRollbackEarlyRepaymentDebt(moneyOperItemId: UUID): List<MoneyOper>
}