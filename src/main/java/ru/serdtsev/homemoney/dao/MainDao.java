package ru.serdtsev.homemoney.dao;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import ru.serdtsev.homemoney.dto.*;

import java.beans.PropertyVetoException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class MainDao {
  private static MainDao ourInstance = new MainDao();
  private static ComboPooledDataSource cpds;
  private static String baseBsSelectQuery =
      "select id, created_ts as createdTs, svc_rsv_id as svcRsvId, " +
          "uncat_costs_id as uncatCostsId, uncat_income_id as uncatIncomeId " +
          "from balance_sheets ";

  private MainDao() {
    cpds = new ComboPooledDataSource();
    try {
      cpds.setDriverClass("org.postgresql.Driver");
      cpds.setJdbcUrl("jdbc:postgresql://localhost:5433/homemoney" );
      cpds.setUser("postgres");
      cpds.setPassword("manager");
      cpds.setMinPoolSize(5);
      cpds.setAcquireIncrement(5);
      cpds.setMaxPoolSize(5);
    } catch (PropertyVetoException e) {
      e.printStackTrace();
    }
  }

  public static MainDao getInstance() {
    return ourInstance;
  }

  public static Connection getConnection() {
    try {
      Connection conn = cpds.getConnection();
      conn.setAutoCommit(false);
      return conn;
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static void clearDatabase() {
    try (Connection conn = MainDao.getConnection()) {
      QueryRunner run = new QueryRunner();
      run.update(conn, "delete from money_trns");
      run.update(conn, "delete from balances");
      run.update(conn, "delete from reserves");
      run.update(conn, "delete from accounts");
      run.update(conn, "delete from users");
      run.update(conn, "delete from balance_sheets");
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static List<BalanceSheet> getBalanceSheets() {
    List<BalanceSheet> list;
    try (Connection conn = MainDao.getConnection()) {
      ResultSetHandler<List<BalanceSheet>> h = new BeanListHandler<>(BalanceSheet.class);
      QueryRunner run = new QueryRunner();
      list = run.query(conn, baseBsSelectQuery + "order by created_ts desc", h);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return list;
  }

  public static BalanceSheet getBalanceSheet(UUID id) {
    BalanceSheet balanceSheet;
    try (Connection conn = MainDao.getConnection()) {
      balanceSheet = getBalanceSheet(conn, id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return balanceSheet;
  }

  static BalanceSheet getBalanceSheet(Connection conn, UUID id) throws SQLException {
    QueryRunner run = new QueryRunner();
    ResultSetHandler<BalanceSheet> h = new BeanHandler<>(BalanceSheet.class);
    return run.query(conn, baseBsSelectQuery + "where id = ?", h, id);
  }

  public static void createBalanceSheet(UUID id) {
    try (Connection conn = MainDao.getConnection()) {
      createBalanceSheet(conn, id);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  static void createBalanceSheet(Connection conn, UUID id) throws SQLException {
    QueryRunner run = new QueryRunner();
    java.sql.Timestamp now = new java.sql.Timestamp(new java.util.Date().getTime());
    run.update(conn, "insert into balance_sheets(id, created_ts) values (?, ?)", id, now);

    UUID svcRsvId = UUID.randomUUID();
    AccountsDao.createAccount(conn, id,
        new Account(svcRsvId, Account.Type.service, "Service reserve"));
    run.update(conn, "update balance_sheets set svc_rsv_id = ? where id = ?", svcRsvId, id);

    UUID uncatCostsId = UUID.randomUUID();
    AccountsDao.createAccount(conn, id,
        new Account(uncatCostsId, Account.Type.expense, "<Без категории>"));
    run.update(conn, "update balance_sheets set uncat_costs_id = ? where id = ?", uncatCostsId, id);

    UUID uncatIncomeId = UUID.randomUUID();
    AccountsDao.createAccount(conn, id,
        new Account(uncatIncomeId, Account.Type.income, "<Без категории>"));
    run.update(conn, "update balance_sheets set uncat_income_id = ? where id = ?", uncatIncomeId, id);

    BalancesDao.createBalance(conn, id,
        new Balance(UUID.randomUUID(), Account.Type.debit, "Наличные", BigDecimal.ZERO, null, null, null));
  }

  public static void deleteBalanceSheet(UUID id) {
    try (Connection conn = MainDao.getConnection()) {
      QueryRunner run = new QueryRunner();
      run.update(conn, "delete from accounts where balance_sheet_id = ?", id);
      run.update(conn, "delete from balance_sheets where id = ?", id);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static BsStat getBsStat(UUID bsId, Long interval) {
    LocalDate today = LocalDate.now();
    Date toDate = Date.valueOf(today);
    Date fromDate = Date.valueOf(today.minusDays(interval));

    BsStat bsStat = new BsStat(bsId, fromDate, toDate);
    try (Connection conn = MainDao.getConnection()) {
      QueryRunner run = new QueryRunner();
      ResultSetHandler<List<Turnover>> handler = new BeanListHandler<>(Turnover.class);

      calcCurrSaldo(conn, run, bsStat);

      Map<Date, BsDayStat> map = new TreeMap<>();
      fillBsDayStatMap(map,
          getRealTurnovers(conn, run, handler, bsId, MoneyTrn.Status.done, fromDate, toDate));
      calcPastSaldoNTurnovers(bsStat, map);

      LocalDate trendFromLocalDate = today.plusDays(1).minusMonths(1);
      Date trendFromDate = Date.valueOf(trendFromLocalDate);
      Date trendToDate = Date.valueOf(trendFromLocalDate.plusDays(interval-1));
      Map<Date, BsDayStat> trendMap = new TreeMap<>();
      fillBsDayStatMap(trendMap,
          getTrendTurnovers(conn, run, handler, bsId, trendFromDate, trendToDate));
      fillBsDayStatMap(trendMap,
          getRealTurnovers(conn, run, handler, bsId, MoneyTrn.Status.pending,
              Date.valueOf(LocalDate.of(1970, 1, 1)), Date.valueOf(today.plusDays(interval))));
      fillBsDayStatMap(trendMap, getTemplTurnovers(bsId, Date.valueOf(today.plusDays(interval))));
      calcTrendSaldoNTurnovers(bsStat, trendMap);

      map.putAll(trendMap);
      bsStat.setDayStats(new ArrayList<>(map.values()));
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return bsStat;
  }

  /**
   * Вычисляет текущие балансы счетов и резервов.
   */
  private static void calcCurrSaldo(Connection conn, QueryRunner run, BsStat bsStat)
      throws SQLException {
    ResultSetHandler<List<AggrAccSaldo>> aggrAccBalanceHandler = new BeanListHandler<>(AggrAccSaldo.class);
    List<AggrAccSaldo> aggrAccSaldo = run.query(conn,
        "select a.type, sum(b.value) as saldo " +
            "  from accounts a, balances b " +
            "  where a.balance_sheet_id = ? and a.type in ('debit', 'credit', 'reserve', 'asset') and b.id = a.id " +
            "  group by type",
        aggrAccBalanceHandler, bsStat.getBsId());
    for (AggrAccSaldo saldo : aggrAccSaldo) {
      bsStat.getSaldoMap().put(saldo.getType(), saldo.getSaldo());
    }
  }

  private static void calcPastSaldoNTurnovers(BsStat bsStat, Map<Date, BsDayStat> map) {
    Map<Account.Type, BigDecimal> saldoMap = new HashMap<>(Account.Type.values().length);
    bsStat.getSaldoMap().forEach((type, value) -> saldoMap.put(type, value.plus()));
    List<BsDayStat> dayStats = new ArrayList<>(map.values());
    dayStats.sort((e1, e2) -> e1.getDateAsLocalDate().isAfter(e2.getDateAsLocalDate()) ? -1 : 1);
    dayStats.forEach(dayStat -> {
      Arrays.asList(Account.Type.values()).forEach(type -> {
        dayStat.setSaldo(type, saldoMap.getOrDefault(type, BigDecimal.ZERO));
        saldoMap.put(type, saldoMap.getOrDefault(type, BigDecimal.ZERO).subtract(dayStat.getDelta(type)));
      });
      bsStat.setIncomeAmount(bsStat.getIncomeAmount().add(dayStat.getIncomeAmount()));
      bsStat.setChargesAmount(bsStat.getChargesAmount().add(dayStat.getChargeAmount()));
    });
  }

  private static void calcTrendSaldoNTurnovers(BsStat bsStat, Map<Date, BsDayStat> trendMap) {
    ArrayList<BsDayStat> dayStats = new ArrayList<>(trendMap.values());
    Map<Account.Type, BigDecimal> saldoMap = new HashMap<>(Account.Type.values().length);
    bsStat.getSaldoMap().forEach((type, value) -> saldoMap.put(type, value.plus()));
    dayStats.forEach(dayStat ->
      Arrays.asList(Account.Type.values()).forEach(type -> {
        BigDecimal saldo = saldoMap.getOrDefault(type, BigDecimal.ZERO).add(dayStat.getDelta(type));
        saldoMap.put(type, saldo);
        dayStat.setSaldo(type, saldo);
      })
    );
  }

  /**
   * Заполняет карту экземпляров BsDayStat суммами из оборотов.
   */
  private static void fillBsDayStatMap(Map<Date, BsDayStat> map, List<Turnover> turnovers) {
    turnovers.forEach(t -> {
      map.putIfAbsent(t.getTrnDate(), new BsDayStat(t.getTrnDate().getTime()));
      BsDayStat dayStat = map.get(t.getTrnDate());
      dayStat.setDelta(t.getFromAccType(), dayStat.getDelta(t.getFromAccType()).subtract(t.getAmount()));
      dayStat.setDelta(t.getToAccType(), dayStat.getDelta(t.getToAccType()).add(t.getAmount()));
      if (Account.Type.income.equals(t.getFromAccType())) {
        dayStat.setIncomeAmount(dayStat.getIncomeAmount().add(t.getAmount()));
      }
      if (Account.Type.expense.equals(t.getToAccType())) {
        dayStat.setChargeAmount(dayStat.getChargeAmount().add(t.getAmount()));
      }
    });
  }

  private static List<Turnover> getRealTurnovers(Connection conn, QueryRunner run,
      ResultSetHandler<List<Turnover>> handler, UUID bsId, MoneyTrn.Status status,
      Date fromDate, Date toDate) {
    try {
      return run.query(conn,
            "select mt.trn_date as trnDate, " +
                " af.type as fromAccType, at.type as toAccType, sum(mt.amount) as amount " +
                "  from money_trns mt, accounts af, accounts at " +
                "  where mt.balance_sheet_id = ? and status = ? and mt.trn_date between ? and ? " +
                "    and af.id = mt.from_acc_id and at.id = mt.to_acc_id " +
                "  group by mt.trn_date, af.type, at.type ",
                handler, bsId, status.name(), fromDate, toDate
            );
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private static List<Turnover> getTrendTurnovers(Connection conn, QueryRunner run,
      ResultSetHandler<List<Turnover>> handler, UUID bsId, Date fromDate, Date toDate) {
    try {
      return run.query(conn,
          "select mt.trn_date + interval '1 months' as trnDate, " +
              " af.type as fromAccType, at.type as toAccType, " +
              " sum(case when mt.period = 'single' or not mt.templ_id is null then 0 else mt.amount end) as amount " +
              "  from money_trns mt, accounts af, accounts at " +
              "  where mt.balance_sheet_id = ? and status = ? and mt.trn_date between ? and ? " +
              "    and af.id = mt.from_acc_id and at.id = mt.to_acc_id " +
              "  group by mt.trn_date, af.type, at.type ",
          handler, bsId, MoneyTrn.Status.done.name(), fromDate, toDate
      );
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private static List<Turnover> getTemplTurnovers(UUID bsId, Date toDate) {
    List<MoneyTrnTempl> templs = MoneyTrnTemplsDao.getMoneyTrnTempls(bsId, Optional.empty());
    Set<Turnover> turnovers = new HashSet<>();
    Date today = Date.valueOf(LocalDate.now());
    templs.stream().forEach(t -> {
      Date templNextDate = t.getNextDate();
      while (templNextDate.compareTo(toDate) <= 0) {
        Account fromAcc = AccountsDao.getAccount(t.getFromAccId());
        Account toAcc = AccountsDao.getAccount(t.getToAccId());
        Date nextDate = templNextDate.before(today) ? today : templNextDate;
        final Turnover newTurnover = new Turnover(nextDate, fromAcc.getType(), toAcc.getType());
        Turnover turnover = newTurnover;
        if (turnovers.contains(newTurnover)) {
          turnover = turnovers.stream().filter(t1 -> t1.equals(newTurnover)).findAny().get();
        } else {
          turnovers.add(turnover);
        }
        turnover.setAmount(turnover.getAmount().add(t.getAmount()));
        templNextDate = MoneyTrnTempl.Companion.calcNextDate(templNextDate, t.getPeriod());
      }
    });
    return new ArrayList<>(turnovers);
  }

}
