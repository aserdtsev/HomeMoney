package ru.serdtsev.homemoney.balancesheet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.balancesheet.model.*
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.config.CoroutineApiRequestContext
import ru.serdtsev.homemoney.moneyoper.dao.MoneyOperDao
import ru.serdtsev.homemoney.moneyoper.dao.MoneyOperItemDao
import ru.serdtsev.homemoney.moneyoper.dao.RecurrenceOperDao
import ru.serdtsev.homemoney.moneyoper.dao.TagDao
import ru.serdtsev.homemoney.moneyoper.model.CategoryType
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperType
import ru.serdtsev.homemoney.moneyoper.model.Period
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional(readOnly = true)
class StatService(
    private val balanceSheetDao: BalanceSheetDao,
    private val tagDao: TagDao,
    private val moneyOperDao: MoneyOperDao,
    private val moneyOperItemDao: MoneyOperItemDao,
    private val recurrenceOperDao: RecurrenceOperDao,
    private val apiRequestContextHolder: ApiRequestContextHolder
) {
    private val log = KotlinLogging.logger {  }

    fun getBsStat(interval: Long): BsStat {
        return runBlocking(Dispatchers.Default + CoroutineApiRequestContext()) {
            val balanceSheet = apiRequestContextHolder.getBalanceSheet()

            val trendTurnovers = async {
                getTrendTurnovers(balanceSheet, interval)
            }

            val today = LocalDate.now()
            val fromDate = today.minusDays(interval)

            val bsStat = BsStat(fromDate, today)
            calcCurrentSaldo(bsStat)

            val trendMap = TreeMap<LocalDate, BsDayStat>()

            val realTurnovers = getRealTurnovers(balanceSheet, MoneyOperStatus.done, fromDate, today)
            val pendingTurnovers = getRealTurnovers(balanceSheet, MoneyOperStatus.pending,
                    LocalDate.ofEpochDay(0), today.plusDays(interval))
            val recurrenceTurnovers = getRecurrenceTurnovers(balanceSheet, today.plusDays(interval))

            val map = TreeMap<LocalDate, BsDayStat>()
            fillBsDayStatMap(map, realTurnovers)
            calcPastSaldoAndTurnovers(bsStat, map)

            fillBsDayStatMap(trendMap, pendingTurnovers)
            fillBsDayStatMap(trendMap, recurrenceTurnovers)
            fillBsDayStatMap(trendMap, trendTurnovers.await())
            calcTrendSaldoAndTurnovers(bsStat, trendMap)

            map.putAll(trendMap)
            bsStat.dayStats = ArrayList(map.values)
            bsStat.categories = getCategories(balanceSheet, fromDate, today)

            bsStat
        }
    }

    /**
     * Вычисляет текущие балансы счетов и резервов.
     */
    private fun calcCurrentSaldo(bsStat: BsStat) {
        val bsId = apiRequestContextHolder.getBsId()
        balanceSheetDao.getAggregateAccountSaldoList(bsId).forEach { bsStat.saldoMap[it.first] = it.second }
    }

    private fun calcPastSaldoAndTurnovers(bsStat: BsStat, bsDayStatMap: Map<LocalDate, BsDayStat>) {
        val cursorSaldoMap =  HashMap<AccountType, BigDecimal>(AccountType.values().size)
        bsStat.saldoMap.forEach { (type, value) -> cursorSaldoMap[type] = value }
        val dayStats = ArrayList(bsDayStatMap.values)
        dayStats.sortByDescending { it.localDate }
        dayStats.forEach { dayStat ->
            AccountType.values().forEach { type ->
                val saldo = cursorSaldoMap.getOrDefault(type, BigDecimal.ZERO)
                dayStat.setSaldo(type, saldo)
                cursorSaldoMap[type] = saldo.subtract(dayStat.getDelta(type))
            }
            bsStat.incomeAmount = bsStat.incomeAmount + dayStat.incomeAmount
            bsStat.chargesAmount = bsStat.chargesAmount + dayStat.chargeAmount
        }
    }

    private fun calcTrendSaldoAndTurnovers(bsStat: BsStat, trendMap: Map<LocalDate, BsDayStat>) {
        val dayStats = ArrayList(trendMap.values)
        val saldoMap = HashMap<AccountType, BigDecimal>(AccountType.values().size)
        bsStat.saldoMap.forEach { (type, value) -> saldoMap[type] = value }
        dayStats.forEach { dayStat ->
            AccountType.values().forEach { type ->
                val saldo = (saldoMap as Map<AccountType, BigDecimal>).getOrDefault(type, BigDecimal.ZERO) + dayStat.getDelta(type)
                saldoMap[type] = saldo
                dayStat.setSaldo(type, saldo)
            }
        }
    }

    /**
     * Заполняет ассоциированный массив экземпляров BsDayStat суммами из оборотов.
     */
    private fun fillBsDayStatMap(map: MutableMap<LocalDate, BsDayStat>, turnovers: Collection<Turnover>) {
        turnovers.forEach { (operDate, turnoverType, amount) ->
            val dayStat = map.computeIfAbsent(operDate) { BsDayStat(operDate) }
            when (turnoverType) {
                TurnoverType.income ->
                    dayStat.incomeAmount = dayStat.incomeAmount.add(amount)
                TurnoverType.expense ->
                    dayStat.chargeAmount = dayStat.chargeAmount.add(amount)
                else -> {
                    val accountType = AccountType.valueOf(turnoverType.name)
                    dayStat.setDelta(accountType, dayStat.getDelta(accountType).add(amount))
                }
            }
        }
    }

    private fun getCategories(balanceSheet: BalanceSheet, fromDate: LocalDate, toDate: LocalDate): List<CategoryStat> {
        val absentCatId = UUID.randomUUID()
        val map = moneyOperItemDao.findByBalanceSheetAndPerformedBetweenAndMoneyOperStatus(balanceSheet, fromDate,
            toDate, MoneyOperStatus.done)
            .map {
                val moneyOper = moneyOperDao.findById(it.moneyOperId)
                Pair(it, moneyOper) }
            .filter {
                val item = it.first
                val moneyOper = it.second
                moneyOper.type == MoneyOperType.expense || item.balance.type == AccountType.reserve
            }
            .map {
                val item = it.first
                val moneyOper = it.second
                val category = moneyOper.tags.firstOrNull { tag -> tag.isCategory!! }
                    ?.let { tag -> tag.rootId?.let { rootId -> tagDao.findByIdOrNull(rootId) } ?: tag }

                val isReserve = item.balance.type == AccountType.reserve

                val id = when {
                    category != null -> category.id
                    isReserve -> item.balance.id
                    else -> absentCatId
                }

                val name = category?.name ?: if (isReserve) item.balance.name else "<Без категории>"
                CategoryStat(id!!, isReserve, name, item.value)
            }
            .groupBy { it }
        map.forEach { (categoryStat, list) ->
            categoryStat.amount = list
                .sumOf { it.amount }
                .let { if (categoryStat.isReserve) it else it.abs() }
        }
        return map.keys.sortedByDescending { it.amount }
    }

    fun getRealTurnovers(balanceSheet: BalanceSheet, status: MoneyOperStatus, fromDate: LocalDate, toDate: LocalDate): Collection<Turnover> {
        log.info { "getRealTurnovers start by $status, ${fromDate.format(DateTimeFormatter.ISO_DATE)} - ${toDate.format(
            DateTimeFormatter.ISO_DATE)}" }

        val turnovers = moneyOperItemDao.findByBalanceSheetAndPerformedBetweenAndMoneyOperStatus(balanceSheet,
            fromDate, toDate, status)
            .map {
                val moneyOper = moneyOperDao.findById(it.moneyOperId)
                Pair(it, moneyOper)
            }
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
                val value = if (item.balance.currencyCode != balanceSheet.currencyCode && moneyOper.isForeignCurrencyTransaction)
                    moneyOper.valueInNationalCurrency.negate()
                else item.value
                val turnoverType = TurnoverType.valueOf(balance.type)
                val turnover = Turnover(item.performed, turnoverType, value)
                itemTurnovers.add(turnover)
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

        turnovers.forEach { (turnover, dayAndTypeTurnovers) -> dayAndTypeTurnovers.forEach { turnover.plus(it.amount) } }

        log.info { "getRealTurnovers finish" }
        return turnovers.keys
    }

    fun getTrendTurnovers(balanceSheet: BalanceSheet, interval: Long): Collection<Turnover> {
        log.info { "getTrendTurnovers start" }
        val today = LocalDate.now()
        val fromDate = today.minusDays(interval)
        val turnovers = moneyOperItemDao.findByBalanceSheetAndPerformedBetweenAndMoneyOperStatus(balanceSheet,
            fromDate, today, MoneyOperStatus.done)
            .map {
                val moneyOper = moneyOperDao.findById(it.moneyOperId)
                Pair(it, moneyOper)
            }
            .filter {
                val item = it.first
                val moneyOper = it.second
                moneyOper.period == Period.month && moneyOper.recurrenceId == null &&
                        moneyOper.type != MoneyOperType.transfer &&
                        item.balance.type.isBalance && item.balance.type != AccountType.reserve

            }
            .sortedBy { it.first.performed }
            .flatMap {
                val item = it.first
                val moneyOper = it.second
                val trendDate = today.plusDays(ChronoUnit.DAYS.between(item.performed, today))
                val turnover1 = Turnover(trendDate, TurnoverType.valueOf(item.balance.type), item.value)
                val turnover2 = Turnover(trendDate, TurnoverType.valueOf(moneyOper.type.name), item.value.abs())
                listOf(turnover1, turnover2)
            }
            .groupBy { Turnover(it.operDate, it.turnoverType) }

        turnovers.forEach { (turnover, dayAndTypeTurnovers) -> dayAndTypeTurnovers.forEach { turnover.plus(it.amount) } }

        val trendTurnovers = turnovers.keys.toMutableList()
        trendTurnovers.sortBy { it.operDate }

        log.info { "getTrendTurnovers finish" }
        return trendTurnovers
    }

    fun getRecurrenceTurnovers(balanceSheet: BalanceSheet, toDate: LocalDate): Collection<Turnover> {
        log.info { "getRecurrenceTurnovers start" }

        val recurrenceOpers = recurrenceOperDao.findByBalanceSheetAndArc(balanceSheet, false)
        val turnovers = HashSet<Turnover>()
        val today = LocalDate.now()
        recurrenceOpers
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