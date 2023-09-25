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
            val balanceSheet = apiRequestContextHolder.getBalanceSheet()

            val fromDate = currentDate.minusDays(interval)
            val toDate = currentDate.plusDays(interval)

            val bsStat = BsStat(fromDate, currentDate)
            calcCurrentSaldo(bsStat)
            bsStat.actualDebt = balanceSheetRepository.getActualDebt(balanceSheet.id)
            bsStat.actualCreditCardDebt = moneyOperRepository.getCurrentCreditCardDebt(currentDate)

            val map = TreeMap<LocalDate, BsDayStat>()
            fillDayStatMap(map, getRealTurnovers(MoneyOperStatus.done, fromDate, currentDate, interval))

            val trendMap = TreeMap<LocalDate, BsDayStat>()
            fillDayStatMap(trendMap, getCreditCardChargesThatAffectPeriodTurnovers(fromDate, toDate))
            fillDayStatMap(trendMap,
                getRealTurnovers(MoneyOperStatus.pending, LocalDate.ofEpochDay(0), toDate, interval))
            fillDayStatMap(trendMap, getRecurrenceTurnovers(currentDate, currentDate.plusDays(interval)))
            fillDayStatMap(trendMap, getTrendTurnovers(currentDate, interval))

            trendMap.forEach { (k, v) ->
                map.merge(k, v) { dayStat1, dayStat2 ->
                    dayStat1.freeCorrection += dayStat2.freeCorrection
                    dayStat1
                }
            }

            correctBsStatInPast(bsStat, map, currentDate)
            correctBsStatInFuture(bsStat, map, currentDate)

            bsStat.dayStats += map.values
            bsStat.categories += getCategories(fromDate, currentDate)

            bsStat
        }
    }

    private fun correctBsStatInFuture(bsStat: BsStat,
        map: TreeMap<LocalDate, BsDayStat>,
        currentDate: LocalDate) {
        var isFirst = true
        var prev: BsDayStat? = null
        val cursorSaldoMap = bsStat.saldoMap
            .map { (type, value) -> type to value }
            .toMap()
            .toMutableMap()
        map.values
            .filter { it.localDate > currentDate }
            .sortedBy { it.localDate }
            .forEach { dayStat ->
                AccountType.values().forEach { type ->
                    val saldo = cursorSaldoMap.getOrDefault(type, BigDecimal.ZERO) + dayStat.getDelta(type)
                    cursorSaldoMap[type] = saldo
                    dayStat.setSaldo(type, saldo)
                }
                if (isFirst) {
                    dayStat.freeCorrection = -bsStat.actualCreditCardDebt + dayStat.freeCorrection
                } else {
                    dayStat.freeCorrection = prev!!.freeCorrection + dayStat.freeCorrection
                }
                isFirst = false
                prev = dayStat
            }
    }

    private fun correctBsStatInPast(bsStat: BsStat, map: Map<LocalDate, BsDayStat>, currentDate: LocalDate) {
        var isFirst = true
        var prev: BsDayStat? = null
        var prevFreeCorrectionDelta = BigDecimal("0.00")
        val cursorSaldoMap = bsStat.saldoMap
            .map { (type, value) -> type to value }
            .toMap()
            .toMutableMap()
        map.values
            .filter { it.localDate <= currentDate }
            .sortedByDescending { it.localDate }
            .forEach { dayStat ->
                AccountType.values().forEach { type ->
                    val saldo =
                        cursorSaldoMap.getOrDefault(type, BigDecimal.ZERO) - (prev?.getDelta(type) ?: BigDecimal.ZERO)
                    dayStat.setSaldo(type, saldo)
                    cursorSaldoMap[type] = saldo
                }
                bsStat.incomeAmount = bsStat.incomeAmount + dayStat.incomeAmount
                bsStat.chargesAmount = bsStat.chargesAmount + dayStat.chargeAmount

                val tmpPrevFreeCorrectionDelta = dayStat.freeCorrection
                if (isFirst) {
                    dayStat.freeCorrection = -bsStat.actualCreditCardDebt
                } else {
                    dayStat.freeCorrection = prev!!.freeCorrection - prevFreeCorrectionDelta
                }
                isFirst = false
                prevFreeCorrectionDelta = tmpPrevFreeCorrectionDelta
                prev = dayStat
            }
    }

    /**
     * Вычисляет текущие балансы счетов и резервов.
     */
    private fun calcCurrentSaldo(bsStat: BsStat) {
        val bsId = apiRequestContextHolder.getBsId()
        balanceSheetRepository.getAggregateAccountSaldoList(bsId).forEach { bsStat.saldoMap[it.first] = it.second }
    }

    /**
     * Создает ассоциированный массив экземпляров BsDayStat и наполняет их суммами из оборотов.
     */
    private fun fillDayStatMap(map: MutableMap<LocalDate, BsDayStat>, turnovers: Collection<Turnover>) {
        turnovers.forEach { (date, turnoverType, amount, freeCorrection) ->
            val dayStat = map.computeIfAbsent(date) {
                BsDayStat(date)
            }
            when (turnoverType) {
                TurnoverType.income ->
                    dayStat.incomeAmount = dayStat.incomeAmount + amount
                TurnoverType.expense ->
                    dayStat.chargeAmount = dayStat.chargeAmount + amount
                else -> {
                    val accountType = AccountType.valueOf(turnoverType.name)
                    dayStat.setDelta(accountType, dayStat.getDelta(accountType) + amount)
                    dayStat.freeCorrection += freeCorrection
                }
            }
        }
    }

    private fun getCategories(fromDate: LocalDate, toDate: LocalDate): List<CategoryStat> {
        val map = moneyOperRepository.findByPerformedBetweenAndMoneyOperStatus(fromDate, toDate,
            MoneyOperStatus.done)
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

        val turnovers =
            moneyOperRepository.findByPerformedBetweenAndMoneyOperStatus(fromDate, currentDate, status)
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
                val freeCorrection = if (item.dateWithGracePeriod > item.performed) item.value.negate() else BigDecimal.ZERO
                val operDate = when (status) {
                    MoneyOperStatus.done -> item.performed
                    MoneyOperStatus.pending -> currentDate
                    else -> throw IllegalArgumentException(status.toString())
                }
                Turnover(operDate, turnoverType, item.value, freeCorrection, true).apply { itemTurnovers.add(this) }
                if (item.dateWithGracePeriod > item.performed && item.dateWithGracePeriod <= currentDate.plusDays(interval)) {
                    // Добавим нулевой оборот на день гашения задолженности по кредитке, чтобы создать на этот день
                    // экземпляр BsDayStat.
                    Turnover(item.dateWithGracePeriod, turnoverType, BigDecimal("0.00"), freeCorrection.negate(), false)
                        .apply { itemTurnovers.add(this) }
                }
                moneyOper.tags
                    .firstOrNull()?.let {
                        if (balance.type == AccountType.debit) {
                            val categoryType = if (item.value.signum() < 0) CategoryType.expense else CategoryType.income
                            itemTurnovers.add(Turnover(item.performed, TurnoverType.valueOf(categoryType), item.value.abs(), isReal = true))
                        }
                    }
                itemTurnovers
            }
            .groupBy { Turnover(it.operDate, it.turnoverType, isReal = it.isReal) }

        turnovers.forEach { (turnover, dayAndTypeTurnovers) ->
            dayAndTypeTurnovers.forEach { turnover.plus(it) } }

        log.info { "getRealTurnovers finish" }
        return turnovers.keys
    }

    private fun getTrendTurnovers(currentDate: LocalDate, interval: Long): Collection<Turnover> {
        log.info { "getTrendTurnovers start" }
        val fromDate = currentDate.minusDays(interval)
        val turnovers = moneyOperRepository.findByPerformedBetweenAndMoneyOperStatus(
            fromDate, currentDate, MoneyOperStatus.done)
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
                val freeCorrection = repaymentScheduleItem?.let {
                    if (repaymentScheduleItem.endDate > trendDate) repaymentScheduleItem.mainDebtAmount.negate()
                    else null
                } ?: BigDecimal.ZERO
                Turnover(trendDate, TurnoverType.valueOf(item.balance.type), item.value, freeCorrection, false)
                    .apply { moneyOperItemsTurnovers.add(this) }
                repaymentScheduleItem?.apply {
                    if (endDate > trendDate && endDate <= currentDate.plusDays(interval)) {
                        // Добавим нулевой оборот на день гашения задолженности по кредитке, чтобы создать на этот день
                        // экземпляр BsDayStat.
                        Turnover(item.dateWithGracePeriod, TurnoverType.valueOf(item.balance.type), BigDecimal("0.00"),
                            freeCorrection.negate(), false)
                            .apply { moneyOperItemsTurnovers.add(this) }
                    }
                }
                Turnover(trendDate, TurnoverType.valueOf(moneyOper.type.name), item.value.abs(), isReal = false)
                    .apply { moneyOperItemsTurnovers.add(this) }
                moneyOperItemsTurnovers
            }
            .groupBy { Turnover(it.operDate, it.turnoverType, isReal = it.isReal) }

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
                while (roNextDate.isBefore(toDate)) {
                    // Если дата повторяющейся операции раньше или равна текущему дню, то считаем, что она будет
                    // выполнена завтра, а не сегодня. Чтобы в графике не искажать баланс текущего дня операциями,
                    // которые, возможно, сегодня не будут выполнены.
                    val nextDate = if (roNextDate.isBefore(currentDate)) currentDate.plusDays(1) else roNextDate
                    it.template.items.forEach { item ->
                        val repaymentScheduleItem = item.balance.credit
                            ?.let { credit -> RepaymentScheduleItem.of(nextDate, credit, item.value) }
                        val freeCorrection = repaymentScheduleItem?.let {
                            if (repaymentScheduleItem.endDate > nextDate) repaymentScheduleItem.mainDebtAmount.negate()
                            else null
                        } ?: BigDecimal.ZERO
                        putRecurrenceTurnover(turnovers, item.value, TurnoverType.valueOf(item.balance.type), nextDate,
                            freeCorrection)
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
        nextDate: LocalDate, freeCorrection: BigDecimal = BigDecimal("0.00")) {
        val turnover = Turnover(nextDate, turnoverType, amount, freeCorrection, isReal = false)
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
                    val freeCorrection = item.value.negate()
                    Turnover(item.dateWithGracePeriod, turnoverType, BigDecimal("0.00"), freeCorrection.negate(), false)
                        .apply { itemTurnovers.add(this) }
                    itemTurnovers
                }
                .groupBy { Turnover(it.operDate, it.turnoverType, isReal = it.isReal) }

        turnovers.forEach { (turnover, dayAndTypeTurnovers) ->
            dayAndTypeTurnovers.forEach { turnover.plus(it) } }

        return turnovers.keys
    }
}
