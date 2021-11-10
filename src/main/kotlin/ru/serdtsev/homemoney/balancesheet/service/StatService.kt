package ru.serdtsev.homemoney.balancesheet.service

import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.balancesheet.dao.BalanceSheetRepo
import ru.serdtsev.homemoney.balancesheet.model.*
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.dao.MoneyOperItemRepo
import ru.serdtsev.homemoney.moneyoper.dao.RecurrenceOperRepo
import ru.serdtsev.homemoney.moneyoper.dao.TagRepo
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
    private val balanceSheetRepo: BalanceSheetRepo,
    private val tagRepo: TagRepo,
    private val moneyOperItemRepo: MoneyOperItemRepo,
    private val recurrenceOperRepo: RecurrenceOperRepo,
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val apiRequestContextHolder: ApiRequestContextHolder
) {
    private val log = KotlinLogging.logger {  }

    fun getBsStat(bsId: UUID, interval: Long): BsStat {
        val balanceSheet = balanceSheetRepo.findByIdOrNull(bsId)!!

        val today = LocalDate.now()
        val fromDate = today.minusDays(interval)

        val bsStat = BsStat(fromDate, today)
        calcCurrentSaldo(bsStat)

        val trendMap = TreeMap<LocalDate, BsDayStat>()

        val realTurnovers = getRealTurnovers(balanceSheet, MoneyOperStatus.done, fromDate, today)
        val pendingTurnovers = getRealTurnovers(balanceSheet, MoneyOperStatus.pending,
                LocalDate.of(1970, 1, 1), today.plusDays(interval))
        val trendTurnovers = getTrendTurnovers(balanceSheet, interval)
        val recurrenceTurnovers = getRecurrenceTurnovers(balanceSheet, today.plusDays(interval))

        val map = TreeMap<LocalDate, BsDayStat>()
        fillBsDayStatMap(map, realTurnovers)
        calcPastSaldoAndTurnovers(bsStat, map)

        fillBsDayStatMap(trendMap, trendTurnovers)
        fillBsDayStatMap(trendMap, pendingTurnovers)
        fillBsDayStatMap(trendMap, recurrenceTurnovers)
        calcTrendSaldoAndTurnovers(bsStat, trendMap)

        map.putAll(trendMap)
        bsStat.dayStats = ArrayList(map.values)
        bsStat.categories = getCategories(balanceSheet, fromDate, today)

        return bsStat
    }

    /**
     * Вычисляет текущие балансы счетов и резервов.
     */
    private fun calcCurrentSaldo(bsStat: BsStat) {
        val sql = """
            select type, sum(saldo) as saldo 
            from v_crnt_saldo_by_base_cry 
            where balance_sheet_id = :bsId 
            group by type
        """.trimIndent()
        val paramMap = mapOf("bsId" to apiRequestContextHolder.getBsId())
        data class AggrAccountSaldo(val type: AccountType, val saldo: BigDecimal)
        val aggrAccSaldoList = jdbcTemplate.query(sql, paramMap) { rs, _ ->
            val type = AccountType.valueOf(rs.getString("type"))
            val saldo = rs.getBigDecimal("saldo")
            AggrAccountSaldo(type, saldo)
        }
        aggrAccSaldoList.forEach { bsStat.saldoMap[it.type] = it.saldo }
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
        val map = moneyOperItemRepo.findByBalanceSheetAndPerformedBetweenAndMoneyOperStatus(balanceSheet,
                fromDate, toDate, MoneyOperStatus.done)
                .filter { item -> item.moneyOper.type == MoneyOperType.expense || item.balance.type == AccountType.reserve }
                .map { item ->
                    val oper = item.moneyOper
                    val category = oper.tags.firstOrNull { it.isCategory!! }
                        ?.let {
                            it.rootId?.let { rootId -> tagRepo.findByIdOrNull(rootId) } ?: it
                        }

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
                    .firstOrNull()?.let {
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

    fun getTrendTurnovers(balanceSheet: BalanceSheet, interval: Long): Collection<Turnover> {
        log.info { "getTrendTurnovers start" }
        val today = LocalDate.now()
        val fromDate = today.minusDays(interval)
        val turnovers = moneyOperItemRepo.findByBalanceSheetAndPerformedBetweenAndMoneyOperStatus(balanceSheet,
            fromDate, today, MoneyOperStatus.done)
            .filter { it.moneyOper.period == Period.month && it.moneyOper.recurrenceId == null }
            .filter { it.moneyOper.type != MoneyOperType.transfer }
            .filter { it.balance.type.isBalance && it.balance.type != AccountType.reserve }
            .sortedBy { it.performed }
            .flatMap { item ->
                val trendDate = today.plusDays(ChronoUnit.DAYS.between(item.performed!!, today))
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

    fun getRecurrenceTurnovers(balanceSheet: BalanceSheet, toDate: LocalDate): Collection<Turnover> {
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
