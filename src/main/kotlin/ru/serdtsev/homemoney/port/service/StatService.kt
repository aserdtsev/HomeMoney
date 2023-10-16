package ru.serdtsev.homemoney.port.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.balancesheet.*
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.domain.repository.BalanceSheetRepository
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.domain.repository.RecurrenceOperRepository
import ru.serdtsev.homemoney.domain.repository.TagRepository
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import ru.serdtsev.homemoney.infra.config.CoroutineApiRequestContext
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional(readOnly = true)
class StatService(
    private val balanceSheetRepository: BalanceSheetRepository,
    private val tagRepository: TagRepository,
    private val moneyOperRepository: MoneyOperRepository,
    private val recurrenceOperRepository: RecurrenceOperRepository,
    private val apiRequestContextHolder: ApiRequestContextHolder
) {
    private val log = KotlinLogging.logger {  }

    @Transactional(readOnly = true)
    fun getBsStat(currentDate: LocalDate, interval: Long): BsStat {
        return runBlocking(Dispatchers.Default + CoroutineApiRequestContext()) {
            val bsId = apiRequestContextHolder.getBsId()

            val fromDate = currentDate.minusDays(interval)

            val categories = getCategories(fromDate, currentDate)
            val actualDebt = balanceSheetRepository.getActualDebt(bsId)
            val currentCreditCardDebt = moneyOperRepository.getCurrentCreditCardDebt(currentDate)
            val bsStat = BsStat(fromDate, currentDate, categories = categories, currentDebt = actualDebt,
                currentCreditCardDebt = currentCreditCardDebt)
            balanceSheetRepository.getAggregateAccountSaldoList(bsId).forEach { bsStat.saldoMap[it.first] = it.second }

            val dayStatMap: Map<LocalDate, BsDayStat> = getDayStatMap(currentDate, interval)
            correctBsStatInPast(bsStat, dayStatMap)
            correctBsStatInFuture(bsStat, dayStatMap)
            bsStat.dayStats = dayStatMap.values.toList()

            bsStat
        }
    }

    private fun getDayStatMap(currentDate: LocalDate, interval: Long):
            Map<LocalDate, BsDayStat> {
        val fromDate = currentDate.minusDays(interval)
        val toDate = currentDate.plusDays(interval)
        val turnovers = getRealTurnovers(MoneyOperStatus.Done, fromDate, currentDate, interval)
            .plus(getCreditCardChargesThatAffectPeriodTurnovers(fromDate, toDate))
            .plus(getRealTurnovers(MoneyOperStatus.Pending, LocalDate.ofEpochDay(0), currentDate, interval))
            .plus(getRecurrenceTurnovers(currentDate, toDate))
            .plus(getTrendTurnovers(currentDate, interval))
        val map = TreeMap<LocalDate, BsDayStat>()
        turnovers.forEach { (date, turnoverType, amount, creditCardDebtDelta) ->
            val dayStat = map.computeIfAbsent(date) {
                BsDayStat(date)
            }
            when (turnoverType) {
                TurnoverType.income -> dayStat.incomeAmount += amount
                TurnoverType.expense -> dayStat.chargeAmount += amount
                else -> {
                    val accountType = AccountType.valueOf(turnoverType.name)
                    dayStat.setDelta(accountType, dayStat.getDelta(accountType) + amount)
                    dayStat.creditCardDebtDelta += creditCardDebtDelta
                }
            }
        }
        return map
    }

    private fun correctBsStatInFuture(bsStat: BsStat, map: Map<LocalDate, BsDayStat>) {
        var prev: BsDayStat? = null
        val cursorSaldoMap = bsStat.saldoMap
            .map { (type, value) -> type to value }
            .toMap()
            .toMutableMap()
        val currentDate = bsStat.toDate
        map.values
            .filter { it.localDate > currentDate }
            .sortedBy { it.localDate }
            .forEach { dayStat ->
                AccountType.values().forEach { type ->
                    val saldo = cursorSaldoMap.getOrDefault(type, BigDecimal.ZERO) + dayStat.getDelta(type)
                    cursorSaldoMap[type] = saldo
                    dayStat.setSaldo(type, saldo)
                }
                dayStat.creditCardDebt = prev?.let { it.creditCardDebt + dayStat.creditCardDebtDelta }
                    ?: (bsStat.currentCreditCardDebt + dayStat.creditCardDebtDelta)
                prev = dayStat
            }
    }

    private fun correctBsStatInPast(bsStat: BsStat, dayStatMap: Map<LocalDate, BsDayStat>) {
        var prev: BsDayStat? = null
        val cursorSaldoMap = bsStat.saldoMap
            .map { (type, value) -> type to value }
            .toMap()
            .toMutableMap()
        val currentDate = bsStat.toDate
        dayStatMap.values
            .filter { it.localDate <= currentDate }
            .sortedByDescending { it.localDate }
            .forEach { dayStat ->
                AccountType.values().forEach { type ->
                    val saldo =
                        cursorSaldoMap.getOrDefault(type, BigDecimal.ZERO) - (prev?.getDelta(type) ?: BigDecimal.ZERO)
                    dayStat.setSaldo(type, saldo)
                    cursorSaldoMap[type] = saldo
                }
                bsStat.incomeAmount += dayStat.incomeAmount
                bsStat.chargesAmount += dayStat.chargeAmount

                dayStat.creditCardDebt = prev?.let { it.creditCardDebt - it.creditCardDebtDelta }
                    ?: bsStat.currentCreditCardDebt
                prev = dayStat
            }
    }

    private fun getCategories(fromDate: LocalDate, toDate: LocalDate): List<CategoryStat> {
        val map = moneyOperRepository.findByPerformedBetweenAndMoneyOperStatus(fromDate, toDate,
            MoneyOperStatus.Done)
            .flatMap { moneyOper -> moneyOper.items.map { Pair(it, moneyOper) } }
            .filter {
                val item = it.first
                val moneyOper = it.second
                moneyOper.type == MoneyOperType.expense || item.balance.type == AccountType.reserve
            }
            .map {
                val item = it.first
                val moneyOper = it.second
                val category = moneyOper.tags.firstOrNull { tag -> tag.isCategory }
                    ?.let { tag -> tag.rootId?.let { rootId -> tagRepository.findByIdOrNull(rootId) } ?: tag }
                val isReserve = item.balance.type == AccountType.reserve
                when {
                    category != null -> CategoryStat.of(category, item.value)
                    isReserve -> CategoryStat.of(item.balance, item.value)
                    else -> CategoryStat.ofAbsentCategory(item.value)
                }
            }
            .groupBy { it }
        map.forEach { (categoryStat, list) ->
            categoryStat.amount = list
                .sumOf { it.amount }
                .let { if (categoryStat.isReserve) it else it.abs() }
        }
        return map.keys.sortedByDescending { it.amount }
    }

    private fun getRealTurnovers(status: MoneyOperStatus, fromDate: LocalDate, currentDate: LocalDate, interval: Long): Collection<Turnover> {
        log.info { "getRealTurnovers start by $status, ${fromDate.format(DateTimeFormatter.ISO_DATE)} - ${currentDate.format(
            DateTimeFormatter.ISO_DATE)}" }

        val toDate = currentDate.plusDays(interval)
        val turnovers =
            moneyOperRepository.findByPerformedBetweenAndMoneyOperStatus(fromDate, toDate, status)
            .flatMap { moneyOper -> moneyOper.items.map { Pair(it, moneyOper) } }
            .filter {
                val item = it.first
                val moneyOper = it.second
                moneyOper.status == status && item.balance.type.isBalance
            }
            .flatMap {
                val item = it.first
                val moneyOper = it.second
                val itemTurnovers = ArrayList<Turnover>()
                val balance = item.balance
                val turnoverType = TurnoverType.valueOf(balance.type)
                val creditCardDebtDelta = if (item.dateWithGracePeriod > item.performed) item.value.negate() else BigDecimal.ZERO
                val operDate = if (status == MoneyOperStatus.Pending && item.performed < currentDate) currentDate
                        else item.performed
                Turnover(operDate, turnoverType, item.value, creditCardDebtDelta).apply { itemTurnovers.add(this) }
                if (creditCardDebtDelta > BigDecimal.ZERO && item.dateWithGracePeriod <= currentDate.plusDays(interval)) {
                    // Добавим день гашения задолженности по кредитке с изменением задолженности по кредитным картам.
                    Turnover(item.dateWithGracePeriod, turnoverType, creditCardDebtDelta = -creditCardDebtDelta)
                        .apply { itemTurnovers.add(this) }
                }
                moneyOper.tags
                    .firstOrNull()?.let {
                        if (balance.type == AccountType.debit) {
                            val categoryType = if (item.value.signum() < 0) CategoryType.expense else CategoryType.income
                            itemTurnovers.add(Turnover(item.performed,
                                TurnoverType.valueOf(categoryType),
                                item.value.abs()))
                        }
                    }
                itemTurnovers
            }
            .groupBy { Turnover(it.operDate, it.turnoverType) }

        turnovers.forEach { (turnover, dayAndTypeTurnovers) ->
            dayAndTypeTurnovers.forEach { turnover.plus(it) } }

        log.info { "getRealTurnovers finish" }
        return turnovers.keys
    }

    private fun getTrendTurnovers(currentDate: LocalDate, interval: Long): Collection<Turnover> {
        log.info { "getTrendTurnovers start" }
        val fromDate = currentDate.minusDays(interval)
        val turnovers = moneyOperRepository.findByPerformedBetweenAndMoneyOperStatus(
            fromDate, currentDate, MoneyOperStatus.Done)
            .flatMap { moneyOper -> moneyOper.items.map { Pair(it, moneyOper) } }
            .filter {
                val item = it.first
                val moneyOper = it.second
                moneyOper.period == Period.month
                        && moneyOper.recurrenceId == null
                        && moneyOper.type != MoneyOperType.transfer
                        && item.balance.type.isBalance
                        && item.balance.type != AccountType.reserve
            }
            .sortedBy { it.first.performed }
            .flatMap {
                val item = it.first
                val moneyOper = it.second
                val moneyOperItemsTurnovers = mutableListOf<Turnover>()
                val trendDate = currentDate.plusDays(ChronoUnit.DAYS.between(item.performed, currentDate))
                val repaymentScheduleItem = item.balance.credit
                    ?.let { credit -> RepaymentScheduleItem.of(trendDate, credit, item.value) }
                val creditCardDebtAmount = repaymentScheduleItem?.let {
                    if (repaymentScheduleItem.endDate > trendDate) repaymentScheduleItem.mainDebtAmount.negate()
                    else null
                } ?: BigDecimal.ZERO
                Turnover(trendDate, TurnoverType.valueOf(item.balance.type), item.value, creditCardDebtAmount)
                    .apply { moneyOperItemsTurnovers.add(this) }
                repaymentScheduleItem?.apply {
                    if (endDate > trendDate && endDate <= currentDate.plusDays(interval)) {
                        // Добавим нулевой оборот на день гашения задолженности по кредитке, чтобы создать на этот день
                        // экземпляр BsDayStat.
                        Turnover(item.dateWithGracePeriod, TurnoverType.valueOf(item.balance.type), BigDecimal("0.00"),
                            creditCardDebtAmount.negate())
                            .apply { moneyOperItemsTurnovers.add(this) }
                    }
                }
                Turnover(trendDate, TurnoverType.valueOf(moneyOper.type.name), item.value.abs())
                    .apply { moneyOperItemsTurnovers.add(this) }
                moneyOperItemsTurnovers
            }
            .groupBy { Turnover(it.operDate, it.turnoverType) }

        turnovers.forEach { (turnover, dayAndTypeTurnovers) ->
            dayAndTypeTurnovers.forEach { turnover.plus(it) } }

        val trendTurnovers = turnovers.keys.toMutableList()
        trendTurnovers.sortBy { it.operDate }

        log.info { "getTrendTurnovers finish" }
        return trendTurnovers
    }

    private fun getRecurrenceTurnovers(currentDate: LocalDate, toDate: LocalDate): Collection<Turnover> {
        log.info { "getRecurrenceTurnovers start" }

        val recurrenceOpers = recurrenceOperRepository.findByBalanceSheetAndArc(false)
        val turnovers = HashSet<Turnover>()
        recurrenceOpers
            .forEach {
                var roNextDate = it.nextDate
                while (roNextDate <= toDate) {
                    // Если дата повторяющейся операции раньше или равна текущему дню, то считаем, что она будет
                    // выполнена завтра, а не сегодня. Чтобы в графике не искажать баланс текущего дня операциями,
                    // которые, возможно, сегодня не будут выполнены.
                    val nextDate = if (roNextDate.isBefore(currentDate)) currentDate.plusDays(1) else roNextDate
                    it.template.items.forEach { item ->
                        val repaymentScheduleItem = item.balance.credit
                            ?.let { credit -> RepaymentScheduleItem.of(nextDate, credit, item.value) }
                        val creditCardDebtAmount = repaymentScheduleItem?.let {
                            if (repaymentScheduleItem.endDate > nextDate) repaymentScheduleItem.mainDebtAmount.negate()
                            else null
                        } ?: BigDecimal.ZERO
                        putRecurrenceTurnover(turnovers, item.value, TurnoverType.valueOf(item.balance.type), nextDate,
                            creditCardDebtAmount)
                        val operType = it.template.type
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
        nextDate: LocalDate, creditCardDebtAmount: BigDecimal = BigDecimal.ZERO) {
        val turnover = Turnover(nextDate, turnoverType, amount, creditCardDebtAmount)
        turnovers.firstOrNull { it == turnover }?.let { it.amount += amount }
            ?: turnovers.add(turnover)
    }

    private fun getCreditCardChargesThatAffectPeriodTurnovers(fromDate: LocalDate, toDate: LocalDate): Collection<Turnover> {
        val turnovers =
            moneyOperRepository.findByCreditCardChargesThatAffectPeriod(fromDate, toDate)
                .flatMap { moneyOper -> moneyOper.items.map { Pair(it, moneyOper) } }
                .filter {
                    val item = it.first
                    item.balance.type.isBalance &&item.dateWithGracePeriod > item.performed
                }
                .flatMap {
                    val item = it.first
                    val itemTurnovers = ArrayList<Turnover>()
                    val balance = item.balance
                    val turnoverType = TurnoverType.valueOf(balance.type)
                    val creditCardDebtAmount = -item.value
                    Turnover(item.dateWithGracePeriod, turnoverType, creditCardDebtDelta = -creditCardDebtAmount)
                        .apply { itemTurnovers.add(this) }
                    itemTurnovers
                }
                .groupBy { Turnover(it.operDate, it.turnoverType) }

        turnovers.forEach { (turnover, dayAndTypeTurnovers) ->
            dayAndTypeTurnovers.forEach { turnover.plus(it) } }

        return turnovers.keys
    }
}
