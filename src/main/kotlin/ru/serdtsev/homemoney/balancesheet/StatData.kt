package ru.serdtsev.homemoney.balancesheet

import mu.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.moneyoper.MoneyOperItemRepo
import ru.serdtsev.homemoney.moneyoper.RecurrenceOperRepo
import ru.serdtsev.homemoney.moneyoper.model.CategoryType
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperType
import ru.serdtsev.homemoney.moneyoper.model.Period
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.CompletableFuture

@Component
@Transactional(readOnly = true)
class StatData(
        private val moneyOperItemRepo: MoneyOperItemRepo,
        private val recurrenceOperRepo: RecurrenceOperRepo) {
    private val log = KotlinLogging.logger {  }

    @Async
    fun getRealTurnoversFuture(balanceSheet: BalanceSheet, status: MoneyOperStatus, fromDate: LocalDate,
            toDate: LocalDate) = CompletableFuture.completedFuture(getRealTurnovers(balanceSheet, status, fromDate, toDate))!!

    private fun getRealTurnovers(balanceSheet: BalanceSheet, status: MoneyOperStatus, fromDate: LocalDate, toDate: LocalDate): Collection<Turnover> {
        log.info { "getRealTurnovers start by $status, ${fromDate.format(DateTimeFormatter.ISO_DATE)} - ${toDate.format(DateTimeFormatter.ISO_DATE)}" }

        val turnovers = moneyOperItemRepo.findByBalanceSheetAndPerformedBetweenAndMoneyOperStatus(balanceSheet,
                fromDate, toDate, status)
                .filter { it.moneyOper.status == status && it.balance.type.isBalance}
                .flatMap { item ->
                    val itemTurnovers = ArrayList<Turnover>()
                    val balance = item.balance
                    val value = if (item.balance.currencyCode != balanceSheet.currencyCode && item.moneyOper.isForeignCurrencyTransaction)
                        item.moneyOper.valueInNationalCurrency.negate()
                    else item.value
                    val turnoverType = TurnoverType.valueOf(balance.type)
                    val turnover = Turnover(item.performed!!, turnoverType, value)
                    itemTurnovers.add(turnover)

                    item.moneyOper.tags
                            .firstOrNull { it.isCategory!! }?.let {
                                if (balance.type == AccountType.debit) {
                                    val categoryType = if (item.value.signum() < 0) CategoryType.expense else CategoryType.income
                                    itemTurnovers.add(Turnover(item.performed!!, TurnoverType.valueOf(categoryType), item.value.abs()))
                                }
                            }

                    itemTurnovers
                }
                .groupBy { Turnover(it.operDate, it.turnoverType) }

        turnovers.forEach { (turnover, dayAndTypeTurnovers) -> dayAndTypeTurnovers.forEach { turnover.plus(it.amount) } }

        log.info { "getRealTurnovers finish" }
        return turnovers.keys
    }

    @Async
    fun getTrendTurnoversFuture(balanceSheet: BalanceSheet, fromDate: LocalDate, toDate: LocalDate) =
            CompletableFuture.completedFuture(getTrendTurnovers(balanceSheet, fromDate, toDate))!!

    private fun getTrendTurnovers(balanceSheet: BalanceSheet, fromDate: LocalDate, toDate: LocalDate): Collection<Turnover> {
        log.info { "getTrendTurnovers start" }
        val turnovers = moneyOperItemRepo.findByBalanceSheetAndPerformedBetweenAndMoneyOperStatus(balanceSheet,
                fromDate, toDate, MoneyOperStatus.done)
                .filter { it.moneyOper.period == Period.month && it.moneyOper.recurrenceId == null }
                .filter { it.moneyOper.type != MoneyOperType.transfer }
                .filter { it.balance.type.isBalance && it.balance.type != AccountType.reserve }
                .sortedBy { it.performed }
                .flatMap { item ->
                    val trendDate = item.performed!!.plusMonths(1)
                    val turnover1 = Turnover(trendDate, TurnoverType.valueOf(item.balance.type), item.value)
                    val turnover2 = Turnover(trendDate, TurnoverType.valueOf(item.moneyOper.type.name), item.value.abs())
                    listOf(turnover1, turnover2)
                }
                .groupBy { Turnover(it.operDate, it.turnoverType) }

        turnovers.forEach { (turnover, dayAndTypeTurnovers) -> dayAndTypeTurnovers.forEach { turnover.plus(it.amount) } }

        val trendTurnovers = turnovers.keys.toMutableList()
        trendTurnovers.sortBy { it.operDate }

        log.info { "getTrendTurnovers finish" }
        return trendTurnovers
    }

    @Async
    fun getRecurrenceTurnoversFuture(balanceSheet: BalanceSheet, toDate: LocalDate) =
            CompletableFuture.completedFuture(getRecurrenceTurnovers(balanceSheet, toDate))!!

    private fun getRecurrenceTurnovers(balanceSheet: BalanceSheet, toDate: LocalDate): Collection<Turnover> {
        log.info { "getRecurrenceTurnovers start" }

        val recurrenceOpers = recurrenceOperRepo.findByBalanceSheet(balanceSheet)
        val turnovers = HashSet<Turnover>()
        val today = LocalDate.now()
        recurrenceOpers
                .filter { !it.arc }
                .forEach {
                    val template = it.template
                    var roNextDate = it.nextDate
                    while (roNextDate.isBefore(toDate)) {
                        // Если дата повторяющейся операции раньше или равно текущему дню, то считаем, что она будет
                        // выполнена завтра, а не сегодня. Чтобы в графике не искажать баланс текущего дня операциями,
                        // которые с большей вероятностью сегодня не будут выполнены.
                        val nextDate = if (roNextDate.isBefore(today)) today.plusDays(1) else roNextDate
                        template.items.forEach { item ->
                            putRecurrenceTurnover(turnovers, item.value, TurnoverType.valueOf(item.balance.type), nextDate)
                            val operType = template.type
                            if (operType != MoneyOperType.transfer) {
                                putRecurrenceTurnover(turnovers, item.value.abs(), TurnoverType.valueOf(operType.name), nextDate)
                            }
                        }
                        roNextDate = it.calcNextDate(nextDate)
                    }
                }

        val recurrenceTurnovers = turnovers.toMutableList()
        recurrenceTurnovers.sortBy { it.operDate }

        log.info { "getRecurrenceTurnovers finish" }
        return recurrenceTurnovers
    }

    private fun putRecurrenceTurnover(turnovers: MutableSet<Turnover>, amount: BigDecimal, turnoverType: TurnoverType,
                                      nextDate: LocalDate) {
        val turnover = Turnover(nextDate, turnoverType, amount)
        turnovers.firstOrNull { it == turnover }?.let { it.amount += amount }
            ?: turnovers.add(turnover)
    }
}
