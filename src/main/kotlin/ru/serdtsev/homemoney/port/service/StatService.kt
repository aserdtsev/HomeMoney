package ru.serdtsev.homemoney.port.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.commons.lang3.ObjectUtils.min
import org.apache.commons.lang3.compare.ComparableUtils.max
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
import ru.serdtsev.homemoney.infra.utils.iterator
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

    fun getBsStat(interval: Long): BsStat {
        return runBlocking(Dispatchers.Default + CoroutineApiRequestContext()) {
            val balanceSheet = apiRequestContextHolder.getBalanceSheet()

            val today = LocalDate.now()
            val fromDate = today.minusDays(interval)
            val toDate = today.plusDays(interval)

            val bsStat = BsStat(fromDate, today)
            calcCurrentSaldo(bsStat)
            bsStat.actualDebt = balanceSheetRepository.getActualDebt(balanceSheet.id)

            val realTurnovers = getRealTurnovers(MoneyOperStatus.done, fromDate, today, interval)
            val pendingTurnovers = getRealTurnovers(MoneyOperStatus.pending, LocalDate.ofEpochDay(0), toDate, interval)
            val recurrenceTurnovers = getRecurrenceTurnovers(balanceSheet, today.plusDays(interval))
            val trendTurnovers = getTrendTurnovers(interval)

            val creditCardDayDebtMap = getCreditCardDayDebtMap(MoneyOperStatus.done, fromDate, toDate)
                .apply {
                    getCreditCardDayDebtMap(MoneyOperStatus.pending, LocalDate.ofEpochDay(0), toDate)
                        .forEach { (date, debt) ->
                            this.compute(date) { _, aDebt -> (aDebt ?: BigDecimal.ZERO) + debt }
                        }
                    getRecurrenceCreditCardDayDebtMap(balanceSheet, toDate)
                        .forEach { (date, debt) ->
                            this.compute(date) { _, aDebt -> (aDebt ?: BigDecimal.ZERO) + debt }
                        }
                    getTrendCreditCardDayDebtMap(interval)
                        .forEach { (date, debt) ->
                            this.compute(date) { _, aDebt -> (aDebt ?: BigDecimal.ZERO) + debt }
                        }
                }

            val map = TreeMap<LocalDate, BsDayStat>()
            fillBsDayStatMap(map, realTurnovers)
            calcPastSaldoAndTurnovers(bsStat, map, creditCardDayDebtMap)

            val trendMap = TreeMap<LocalDate, BsDayStat>()
            fillBsDayStatMap(trendMap, pendingTurnovers)
            fillBsDayStatMap(trendMap, recurrenceTurnovers)
            fillBsDayStatMap(trendMap, trendTurnovers)
            calcFutureSaldoAndTurnovers(bsStat, trendMap, creditCardDayDebtMap)

            trendMap.forEach { k, v ->
                map.merge(k, v) { dayStat1, dayStat2 ->
                    dayStat1.debt = dayStat2.debt
                    dayStat1
                }
            }

            bsStat.dayStats += map.values
            bsStat.categories += getCategories(fromDate, today)
            bsStat.actualDebt += creditCardDayDebtMap.getOrDefault(today, BigDecimal.ZERO)

            bsStat
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
    private fun fillBsDayStatMap(map: MutableMap<LocalDate, BsDayStat>, turnovers: Collection<Turnover>) {
        turnovers.forEach { (date, turnoverType, amount, debt) ->
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
                    dayStat.debt += debt
                }
            }
        }
    }

    private fun calcPastSaldoAndTurnovers(bsStat: BsStat, bsDayStatMap: Map<LocalDate, BsDayStat>,
        creditCardDebtMap: Map<LocalDate, BigDecimal>) {
        val cursorSaldoMap = bsStat.saldoMap
            .map { (type, value) -> type to value }
            .toMap()
            .toMutableMap()
        val dayStats = bsDayStatMap.values.toMutableList()
        dayStats.sortByDescending { it.localDate }
        var prev: BsDayStat? = null
        dayStats.forEach { dayStat ->
            AccountType.values().forEach { type ->
                val saldo = cursorSaldoMap.getOrDefault(type, BigDecimal.ZERO)
                dayStat.setSaldo(type, saldo)
                cursorSaldoMap[type] = saldo - (prev?.getDelta(type) ?: BigDecimal.ZERO)
            }
            bsStat.incomeAmount = bsStat.incomeAmount + dayStat.incomeAmount
            bsStat.chargesAmount = bsStat.chargesAmount + dayStat.chargeAmount
            run {
                // todo учесть начало действия кредитного договора
                val creditCardDebt = creditCardDebtMap.getOrDefault(dayStat.localDate, BigDecimal.ZERO)
                dayStat.debt += bsStat.actualDebt + creditCardDebt
            }
            prev = dayStat
        }
    }

    private fun calcFutureSaldoAndTurnovers(bsStat: BsStat, trendMap: Map<LocalDate, BsDayStat>,
        creditCardDebtMap: Map<LocalDate, BigDecimal>) {
        val dayStats = ArrayList(trendMap.values)
        val cursorSaldoMap = bsStat.saldoMap
            .map { (type, value) -> type to value }
            .toMap()
            .toMutableMap()
        dayStats.forEach { dayStat ->
            AccountType.values().forEach { type ->
                val saldo = cursorSaldoMap.getOrDefault(type, BigDecimal.ZERO) + dayStat.getDelta(type)
                cursorSaldoMap[type] = saldo
                dayStat.setSaldo(type, saldo)
            }
            run {
                // todo учесть окончание действия кредитного договора
                val creditCardDebt = creditCardDebtMap.getOrDefault(dayStat.localDate, BigDecimal.ZERO)
                dayStat.debt += bsStat.actualDebt + creditCardDebt
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

    private fun getRealTurnovers(status: MoneyOperStatus, fromDate: LocalDate, toDate: LocalDate, interval: Long): Collection<Turnover> {
        log.info { "getRealTurnovers start by $status, ${fromDate.format(DateTimeFormatter.ISO_DATE)} - ${toDate.format(
            DateTimeFormatter.ISO_DATE)}" }

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
                Turnover(item.performed, turnoverType, item.value).apply { itemTurnovers.add(this) }
                if (item.dateWithGracePeriod > item.performed && item.dateWithGracePeriod <= toDate.plusDays(interval)) {
                    // Добавим нулевой оборот на день гашения задолженности по кредитке, чтобы создать на этот день
                    // экземпляр BsDayStat.
                    Turnover(item.dateWithGracePeriod, turnoverType, BigDecimal("0.00"))
                        .apply { itemTurnovers.add(this) }
                }
                moneyOper.tags
                    .firstOrNull()?.let {
                        if (balance.type == AccountType.debit) {
                            val categoryType = if (item.value.signum() < 0) CategoryType.expense else CategoryType.income
                            itemTurnovers.add(Turnover(item.performed, TurnoverType.valueOf(categoryType), item.value.abs()))
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

    private fun getTrendTurnovers(interval: Long): Collection<Turnover> {
        log.info { "getTrendTurnovers start" }
        val today = LocalDate.now()
        val fromDate = today.minusDays(interval)
        val turnovers = moneyOperRepository.findByPerformedBetweenAndMoneyOperStatus(
            fromDate, today, MoneyOperStatus.done)
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
                val trendDate = today.plusDays(ChronoUnit.DAYS.between(item.performed, today))
                Turnover(trendDate, TurnoverType.valueOf(item.balance.type), item.value)
                    .apply { moneyOperItemsTurnovers.add(this) }
                if (item.dateWithGracePeriod > item.performed && item.dateWithGracePeriod <= today.plusDays(interval)) {
                    // Добавим нулевой оборот на день гашения задолженности по кредитке, чтобы создать на этот день
                    // экземпляр BsDayStat.
                    Turnover(item.dateWithGracePeriod, TurnoverType.valueOf(item.balance.type), BigDecimal("0.00"))
                        .apply { moneyOperItemsTurnovers.add(this) }
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

    private fun getRecurrenceTurnovers(balanceSheet: BalanceSheet, toDate: LocalDate): Collection<Turnover> {
        log.info { "getRecurrenceTurnovers start" }

        val recurrenceOpers = recurrenceOperRepository.findByBalanceSheetAndArc(balanceSheet, false)
        val turnovers = HashSet<Turnover>()
        val today = LocalDate.now()
        recurrenceOpers
            .forEach {
                var roNextDate = it.nextDate
                while (roNextDate.isBefore(toDate)) {
                    // Если дата повторяющейся операции раньше или равна текущему дню, то считаем, что она будет
                    // выполнена завтра, а не сегодня. Чтобы в графике не искажать баланс текущего дня операциями,
                    // которые с большей вероятностью сегодня не будут выполнены.
                    val nextDate = if (roNextDate.isBefore(today)) today.plusDays(1) else roNextDate
                    it.template.items.forEach { item ->
                        putRecurrenceTurnover(turnovers, item.value, TurnoverType.valueOf(item.balance.type), nextDate)
                        val operType = it.template.type
                        if (operType != MoneyOperType.transfer) {
                            putRecurrenceTurnover(turnovers,
                                item.value.abs(),
                                TurnoverType.valueOf(operType.name),
                                nextDate)
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

    /**
     * Возвращает ассоциативный массив дат и задолженности по кредитным картам
     */
    private fun getCreditCardDayDebtMap(status: MoneyOperStatus, fromDate: LocalDate, toDate: LocalDate):
            MutableMap<LocalDate, BigDecimal> {
        val map = mutableMapOf<LocalDate, BigDecimal>()
        moneyOperRepository.findByCreditCardAndDateBetweenAndMoneyOperStatus(fromDate, toDate, status)
            .flatMap { moneyOper -> moneyOper.items }
            .filter {
                it.dateWithGracePeriod > it.performed
            }
            .forEach {
                val first = max(fromDate, it.performed)
                val last = min(it.dateWithGracePeriod.minusDays(1L), toDate)
                for (date in (first..last).iterator()) {
                    map.compute(date) { _, debt -> (debt ?: BigDecimal.ZERO) + it.value.negate() }
                }
            }
        return map
    }

    private fun getRecurrenceCreditCardDayDebtMap(balanceSheet: BalanceSheet, toDate: LocalDate): Map<LocalDate, BigDecimal> {
        val recurrenceOpers = recurrenceOperRepository.findByBalanceSheetAndArc(balanceSheet, false)
        val map = mutableMapOf<LocalDate, BigDecimal>()
        val today = LocalDate.now()
        recurrenceOpers
            .forEach {
                var roNextDate = it.nextDate
                while (roNextDate.isBefore(toDate)) {
                    // Если дата повторяющейся операции раньше или равна текущему дню, то считаем, что она будет
                    // выполнена завтра, а не сегодня. Чтобы в графике не искажать баланс текущего дня операциями,
                    // которые с большей вероятностью сегодня не будут выполнены.
                    val nextDate = if (roNextDate.isBefore(today)) today.plusDays(1) else roNextDate
                    it.template.items
                        .filter { item -> (item.balance.credit?.gracePeriodDays ?: 0) > 0 && item.value < BigDecimal.ZERO}
                        .forEach { item ->
                            val repaymentScheduleItem = item.balance.credit
                                .let { credit -> RepaymentScheduleItem.of(roNextDate, requireNotNull(credit), item.value) }
                            val debt = repaymentScheduleItem?.let { rsItem ->
                                if (rsItem.endDate.isAfter(roNextDate)) rsItem.mainDebtAmount.abs() else BigDecimal.ZERO
                            } ?: BigDecimal.ZERO
                            val first = max(today, roNextDate)
                            val last = min(repaymentScheduleItem!!.endDate.minusDays(1L), toDate)
                            for (date in (first..last).iterator()) {
                                map.compute(date) { _, aDebt -> (aDebt ?: BigDecimal.ZERO) + debt }
                            }
                        }
                    roNextDate = it.calcNextDate(nextDate)
                }
            }
        return map
    }

    private fun getTrendCreditCardDayDebtMap(interval: Long): Map<LocalDate, BigDecimal> {
        val today = LocalDate.now()
        val fromDate = today.minusDays(interval)
        val toDate = today.plusDays(interval)
        val map = mutableMapOf<LocalDate, BigDecimal>()
        moneyOperRepository.findByPerformedBetweenAndMoneyOperStatus(
            fromDate, today, MoneyOperStatus.done)
            .flatMap { moneyOper -> moneyOper.items.map { Pair(it, moneyOper) } }
            .filter {
                val item = it.first
                val moneyOper = it.second
                moneyOper.period == Period.month
                        && moneyOper.recurrenceId == null
                        && moneyOper.type != MoneyOperType.transfer
                        && item.balance.type.isBalance
                        && item.balance.type != AccountType.reserve
                        && (item.balance.credit?.gracePeriodDays ?: 0) > 0
                        && item.value < BigDecimal.ZERO
            }
            .forEach {
                val item = it.first
                val trendDate = today.plusDays(ChronoUnit.DAYS.between(item.performed, today))
                val repaymentScheduleItem = item.balance.credit
                    ?.let { RepaymentScheduleItem.of(trendDate, it, item.value) }
                val debt = repaymentScheduleItem
                    ?.let { rsItem ->
                        if (rsItem.endDate.isAfter(trendDate)) rsItem.mainDebtAmount.abs() else BigDecimal.ZERO
                    } ?: BigDecimal.ZERO
                val first = max(today, trendDate)
                val last = min(repaymentScheduleItem!!.endDate.minusDays(1L), toDate)
                for (date in (first..last).iterator()) {
                    map.compute(date) { _, aDebt -> (aDebt ?: BigDecimal.ZERO) + debt }
                }
            }
        return map
    }
}
