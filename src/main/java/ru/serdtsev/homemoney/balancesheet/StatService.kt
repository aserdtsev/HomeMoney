package ru.serdtsev.homemoney.balancesheet

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.moneyoper.LabelRepository
import ru.serdtsev.homemoney.moneyoper.MoneyOperItemRepo
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
@Repository
@Transactional(readOnly = true)
open class StatService(
        private val balanceSheetRepo: BalanceSheetRepository,
        private val labelRepository: LabelRepository,
        private val moneyOperItemRepo: MoneyOperItemRepo,
        private val jdbcTemplate: JdbcTemplate,
        private val statData: StatData) {

    data class AggrAccountSaldo(var type: AccountType, var saldo: BigDecimal)

    open fun getBsStat(bsId: UUID, interval: Long?): BsStat {
        val balanceSheet = balanceSheetRepo.findOne(bsId)

        val today = LocalDate.now()
        val fromDate = today.minusDays(interval!!)

        val bsStat = BsStat(bsId, fromDate, today)
        calcCurrentSaldo(bsStat)

        val trendFromDate = today.plusDays(1).minusMonths(1)
        val trendToDate = trendFromDate.plusDays(interval - 1)

        val trendMap = TreeMap<LocalDate, BsDayStat>()

        val realTurnovers = statData.getRealTurnoversFuture(balanceSheet, MoneyOperStatus.done, fromDate, today)
        val pendingTurnovers = statData.getRealTurnoversFuture(balanceSheet, MoneyOperStatus.pending,
                LocalDate.of(1970, 1, 1), today.plusDays(interval))
        val trendTurnovers = statData.getTrendTurnoversFuture(balanceSheet, trendFromDate, trendToDate)
        val recurrenceTurnovers = statData.getRecurrenceTurnoversFuture(balanceSheet, today.plusDays(interval))

        val map = TreeMap<LocalDate, BsDayStat>()
        fillBsDayStatMap(map, realTurnovers.get())
        calcPastSaldoAndTurnovers(bsStat, map)

        fillBsDayStatMap(trendMap, trendTurnovers.get())
        fillBsDayStatMap(trendMap, pendingTurnovers.get())
        fillBsDayStatMap(trendMap, recurrenceTurnovers.get())
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
        val aggrAccSaldoList = jdbcTemplate.query<AggrAccountSaldo>(
                "select type, sum(saldo) as saldo from v_crnt_saldo_by_base_cry where bs_id = ? group by type",
                arrayOf(bsStat.bsId)) { rs, _ ->
            val type = AccountType.valueOf(rs.getString("type"))
            val saldo = rs.getBigDecimal("saldo")
            AggrAccountSaldo(type, saldo)
        }

        aggrAccSaldoList.forEach { bsStat.saldoMap[it.type] = it.saldo }
    }

    private fun calcPastSaldoAndTurnovers(bsStat: BsStat, bsDayStatMap: Map<LocalDate, BsDayStat>) {
        val cursorSaldoMap =  HashMap<AccountType, BigDecimal>(AccountType.values().size)
        bsStat.saldoMap.forEach { type, value -> cursorSaldoMap[type] = value }
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
        bsStat.saldoMap.forEach { type, value -> saldoMap[type] = value }
        dayStats.forEach { dayStat ->
            Arrays.asList(*AccountType.values()).forEach { type ->
                val saldo = (saldoMap as Map<AccountType, BigDecimal>).getOrDefault(type, BigDecimal.ZERO) + dayStat.getDelta(type)
                saldoMap[type] = saldo
                dayStat.setSaldo(type, saldo)
            }
        }
    }

    /**
     * Заполняет карту экземпляров BsDayStat суммами из оборотов.
     */
    private fun fillBsDayStatMap(map: MutableMap<LocalDate, BsDayStat>, turnovers: Collection<Turnover>) {
        turnovers.forEach { (operDate, accountType, amount) ->
            val dayStat = map.computeIfAbsent(operDate) { BsDayStat(operDate) }
            dayStat.setDelta(accountType, dayStat.getDelta(accountType).add(amount))
            if (accountType == AccountType.income) {
                dayStat.incomeAmount = dayStat.incomeAmount.add(amount)
            } else if (accountType in arrayOf(AccountType.expense, AccountType.reserve)) {
                dayStat.chargeAmount = dayStat.chargeAmount.add(amount)
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
                    var category = oper.labels.firstOrNull { it.getIsCategory() }
                    val rootId = category?.rootId
                    if (rootId != null) {
                        category = labelRepository.findOne(rootId)
                    }

                    val isReserveIncrease = item.balance.type == AccountType.reserve && item.value.signum() > 0

                    val id = when {
                        category != null -> category.id
                        isReserveIncrease -> item.balance.id
                        else -> absentCatId
                    }

                    val name = category?.name ?: if (isReserveIncrease) item.balance.name else "<Без категории>"
                    CategoryStat(id, null, name, item.value.abs())
                }
                .groupBy { it }

        map.forEach { categoryStat, list ->
            categoryStat.amount = list.map { it.amount }.reduce { acc, amount -> acc + amount }
        }

        return map.keys.filter { it.rootId == null }.sortedByDescending { it.amount }
    }

}
