package ru.serdtsev.homemoney.dao

import com.mchange.v2.c3p0.ComboPooledDataSource
import org.apache.commons.dbutils.DbUtils
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.ResultSetHandler
import org.apache.commons.dbutils.handlers.BeanHandler
import org.apache.commons.dbutils.handlers.BeanListHandler
import ru.serdtsev.homemoney.dto.*

import java.beans.PropertyVetoException
import java.math.BigDecimal
import java.sql.Connection
import java.sql.Date
import java.sql.SQLException
import java.time.LocalDate
import java.util.*

object MainDao {
  private val cpds: ComboPooledDataSource
  private val baseBsSelectQuery =
      "select id, created_ts as createdTs, svc_rsv_id as svcRsvId, " +
          "uncat_costs_id as uncatCostsId, uncat_income_id as uncatIncomeId " +
          "from balance_sheets "

  init {
    cpds = ComboPooledDataSource()
    try {
      cpds.driverClass = "org.postgresql.Driver"
      cpds.jdbcUrl = "jdbc:postgresql://localhost:5433/homemoney"
      cpds.user = "postgres"
      cpds.password = "manager"
      cpds.minPoolSize = 5
      cpds.acquireIncrement = 5
      cpds.maxPoolSize = 5
    } catch (e: PropertyVetoException) {
      e.printStackTrace()
    }
  }

  fun getConnection() =
      try {
        val conn = cpds.connection
        conn.autoCommit = false
        conn
      } catch (e: SQLException) {
        throw HmSqlException(e)
      }

  val balanceSheets: List<BalanceSheet>
    get() {
      val conn = getConnection()
      return try {
        val h = BeanListHandler(BalanceSheet::class.java)
        QueryRunner().query<List<BalanceSheet>>(conn, baseBsSelectQuery + "order by created_ts desc", h)
      } catch (e: SQLException) {
        throw HmSqlException(e)
      } finally {
        DbUtils.close(conn)
      }
    }

  fun clearDatabase() {
    val conn = getConnection()
    try {
      val run = QueryRunner()
      run.update(conn, "delete from money_trns")
      run.update(conn, "delete from balances")
      run.update(conn, "delete from reserves")
      run.update(conn, "delete from accounts")
      run.update(conn, "delete from users")
      run.update(conn, "delete from balance_sheets")
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  fun getBalanceSheet(id: UUID): BalanceSheet {
    val conn = getConnection()
    return try {
      getBalanceSheet(conn, id)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  @Throws(SQLException::class)
  internal fun getBalanceSheet(conn: Connection, id: UUID) =
    QueryRunner().query(conn, baseBsSelectQuery + "where id = ?", BeanHandler(BalanceSheet::class.java), id)

  fun createBalanceSheet(id: UUID) {
    val conn = getConnection()
    try {
      createBalanceSheet(conn, id)
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  @Throws(SQLException::class)
  internal fun createBalanceSheet(conn: Connection, id: UUID) {
    val run = QueryRunner()
    val now = java.sql.Timestamp(java.util.Date().time)
    run.update(conn, "insert into balance_sheets(id, created_ts) values (?, ?)", id, now)

    val svcRsvId = UUID.randomUUID()
    AccountsDao.createAccount(conn, id,
        Account(svcRsvId, Account.Type.service, "Service reserve"))
    run.update(conn, "update balance_sheets set svc_rsv_id = ? where id = ?", svcRsvId, id)

    val uncatCostsId = UUID.randomUUID()
    AccountsDao.createAccount(conn, id,
        Account(uncatCostsId, Account.Type.expense, "<Без категории>"))
    run.update(conn, "update balance_sheets set uncat_costs_id = ? where id = ?", uncatCostsId, id)

    val uncatIncomeId = UUID.randomUUID()
    AccountsDao.createAccount(conn, id,
        Account(uncatIncomeId, Account.Type.income, "<Без категории>"))
    run.update(conn, "update balance_sheets set uncat_income_id = ? where id = ?", uncatIncomeId, id)

    BalancesDao.createBalance(conn, id,
        Balance(UUID.randomUUID(), Account.Type.debit, "Наличные", BigDecimal.ZERO))
  }

  fun deleteBalanceSheet(id: UUID) {
    val conn = getConnection()
    try {
      val run = QueryRunner()
      run.update(conn, "delete from accounts where balance_sheet_id = ?", id)
      run.update(conn, "delete from balance_sheets where id = ?", id)
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  fun getBsStat(bsId: UUID, interval: Long?): BsStat {
    val today = LocalDate.now()
    val toDate = Date.valueOf(today)
    val fromDate = Date.valueOf(today.minusDays(interval!!))

    val bsStat = BsStat(bsId, fromDate, toDate)
    val conn = getConnection()
    try {
      val run = QueryRunner()
      val handler = BeanListHandler(Turnover::class.java)

      calcCurrSaldo(conn, run, bsStat)

      val map = TreeMap<Date, BsDayStat>()
      fillBsDayStatMap(map,
          getRealTurnovers(conn, run, handler, bsId, MoneyTrn.Status.done, fromDate, toDate))
      calcPastSaldoNTurnovers(bsStat, map)

      val trendFromLocalDate = today.plusDays(1).minusMonths(1)
      val trendFromDate = Date.valueOf(trendFromLocalDate)
      val trendToDate = Date.valueOf(trendFromLocalDate.plusDays(interval - 1))
      val trendMap = TreeMap<Date, BsDayStat>()
      fillBsDayStatMap(trendMap,
          getTrendTurnovers(conn, run, handler, bsId, trendFromDate, trendToDate))
      fillBsDayStatMap(trendMap,
          getRealTurnovers(conn, run, handler, bsId, MoneyTrn.Status.pending,
              Date.valueOf(LocalDate.of(1970, 1, 1)), Date.valueOf(today.plusDays(interval))))
      fillBsDayStatMap(trendMap, getTemplTurnovers(bsId, Date.valueOf(today.plusDays(interval))))
      calcTrendSaldoNTurnovers(bsStat, trendMap)

      map.putAll(trendMap)
      bsStat.dayStats = ArrayList(map.values)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }

    return bsStat
  }

  /**
   * Вычисляет текущие балансы счетов и резервов.
   */
  @Throws(SQLException::class)
  private fun calcCurrSaldo(conn: Connection, run: QueryRunner, bsStat: BsStat) {
    val aggrAccSaldo = run.query(conn,
        "select a.type, sum(b.value) as saldo " +
            "  from accounts a, balances b " +
            "  where a.balance_sheet_id = ? and a.type in ('debit', 'credit', 'reserve', 'asset') and b.id = a.id " +
            "  group by type",
        BeanListHandler(AggrAccSaldo::class.java), bsStat.bsId)
    aggrAccSaldo.forEach { saldo -> bsStat.saldoMap.put(saldo.type!!, saldo.saldo!!) }
  }

  private fun calcPastSaldoNTurnovers(bsStat: BsStat, map: Map<Date, BsDayStat>) {
    val saldoMap = HashMap<Account.Type, BigDecimal>(Account.Type.values().size)
    bsStat.saldoMap.forEach { type, value -> saldoMap.put(type, value.plus()) }
    val dayStats = ArrayList(map.values)
    dayStats.sort { e1, e2 -> if (e1.getDateAsLocalDate().isAfter(e2.getDateAsLocalDate())) -1 else 1 }
    dayStats.forEach { dayStat ->
      Arrays.asList(*Account.Type.values()).forEach { type ->
        dayStat.setSaldo(type, saldoMap.getOrDefault(type, BigDecimal.ZERO))
        saldoMap.put(type, (saldoMap).getOrDefault(type, BigDecimal.ZERO).subtract(dayStat.getDelta(type)))
      }
      bsStat.incomeAmount = bsStat.incomeAmount.add(dayStat.incomeAmount)
      bsStat.chargesAmount = bsStat.chargesAmount.add(dayStat.chargeAmount)
    }
  }

  private fun calcTrendSaldoNTurnovers(bsStat: BsStat, trendMap: Map<Date, BsDayStat>) {
    val dayStats = ArrayList(trendMap.values)
    val saldoMap = HashMap<Account.Type, BigDecimal>(Account.Type.values().size)
    bsStat.saldoMap.forEach { type, value -> saldoMap.put(type, value.plus()) }
    dayStats.forEach { dayStat ->
      Arrays.asList(*Account.Type.values()).forEach { type ->
        val saldo = saldoMap.getOrDefault(type, BigDecimal.ZERO).add(dayStat.getDelta(type))
        saldoMap.put(type, saldo)
        dayStat.setSaldo(type, saldo)
      }
    }
  }

  /**
   * Заполняет карту экземпляров BsDayStat суммами из оборотов.
   */
  private fun fillBsDayStatMap(map: MutableMap<Date, BsDayStat>, turnovers: List<Turnover>) {
    turnovers.forEach { t ->
      (map as java.util.Map<Date, BsDayStat>).putIfAbsent(t.trnDate, BsDayStat(t.trnDate!!.time))
      val dayStat = map[t.trnDate!!]!!
      dayStat.setDelta(t.fromAccType!!, dayStat.getDelta(t.fromAccType!!).subtract(t.amount))
      dayStat.setDelta(t.toAccType!!, dayStat.getDelta(t.toAccType!!).add(t.amount))
      if (Account.Type.income == t.fromAccType) {
        dayStat.incomeAmount = dayStat.incomeAmount.add(t.amount)
      }
      if (Account.Type.expense == t.toAccType) {
        dayStat.chargeAmount = dayStat.chargeAmount.add(t.amount)
      }
    }
  }

  private fun getRealTurnovers(conn: Connection, run: QueryRunner,
      handler: ResultSetHandler<List<Turnover>>, bsId: UUID, status: MoneyTrn.Status,
      fromDate: Date, toDate: Date): List<Turnover> {
    try {
      return run.query(conn,
          "select mt.trn_date as trnDate, " +
              " af.type as fromAccType, at.type as toAccType, sum(mt.amount) as amount " +
              "  from money_trns mt, accounts af, accounts at " +
              "  where mt.balance_sheet_id = ? and status = ? and mt.trn_date between ? and ? " +
              "    and af.id = mt.from_acc_id and at.id = mt.to_acc_id " +
              "  group by mt.trn_date, af.type, at.type ",
          handler, bsId, status.name, fromDate, toDate)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    }
  }

  private fun getTrendTurnovers(conn: Connection, run: QueryRunner,
      handler: ResultSetHandler<List<Turnover>>, bsId: UUID, fromDate: Date, toDate: Date): List<Turnover> {
    try {
      return run.query(conn,
          "select mt.trn_date + interval '1 months' as trnDate, " +
              " af.type as fromAccType, at.type as toAccType, " +
              " sum(case when mt.period = 'single' or not mt.templ_id is null then 0 else mt.amount end) as amount " +
              "  from money_trns mt, accounts af, accounts at " +
              "  where mt.balance_sheet_id = ? and status = ? and mt.trn_date between ? and ? " +
              "    and af.id = mt.from_acc_id and at.id = mt.to_acc_id " +
              "  group by mt.trn_date, af.type, at.type ",
          handler, bsId, MoneyTrn.Status.done.name, fromDate, toDate)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    }
  }

  private fun getTemplTurnovers(bsId: UUID, toDate: Date): List<Turnover> {
    val templs = MoneyTrnTemplsDao.getMoneyTrnTempls(bsId, Optional.empty<String>())
    val turnovers = HashSet<Turnover>()
    val today = Date.valueOf(LocalDate.now())
    templs.forEach { t ->
      var templNextDate: Date = t.nextDate!!
      while (templNextDate.compareTo(toDate) <= 0) {
        val fromAcc = AccountsDao.getAccount(t.fromAccId!!)
        val toAcc = AccountsDao.getAccount(t.toAccId!!)
        val nextDate = if (templNextDate.before(today)) today else templNextDate
        val newTurnover = Turnover(nextDate, fromAcc.type, toAcc.type)
        var turnover = newTurnover
        if (turnovers.contains(newTurnover)) {
          turnover = turnovers.filter({ t1 -> t1 == newTurnover }).first()
        } else {
          turnovers.add(turnover)
        }
        turnover.amount = turnover.amount.add(t.amount)
        templNextDate = MoneyTrnTempl.calcNextDate(templNextDate, t.period!!)
      }
    }
    return ArrayList(turnovers)
  }

}