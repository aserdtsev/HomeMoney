package ru.serdtsev.homemoney.dao;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.serdtsev.homemoney.dto.*;

import java.beans.PropertyVetoException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class MainDao {
  private static Logger log = LoggerFactory.getLogger(MainDao.class);
  private static final ComboPooledDataSource cpds = new ComboPooledDataSource();
  private static String baseBsSelectQuery = "" +
      "select id, created_ts as createdTs, svc_rsv_id as svcRsvId, " +
      "    uncat_costs_id as uncatCostsId, uncat_income_id as uncatIncomeId, currency_code as currencyCode " +
      "  from balance_sheets ";

  static {
    try {
      cpds.setDriverClass("org.postgresql.Driver");
      cpds.setJdbcUrl("jdbc:postgresql://localhost:5433/homemoney");
      cpds.setUser("postgres");
      cpds.setPassword("manager");
      cpds.setMinPoolSize(5);
      cpds.setMaxPoolSize(10);
      cpds.setInitialPoolSize(5);
      cpds.setAcquireIncrement(5);
      cpds.setPreferredTestQuery("select 'x' from dual");
    } catch (PropertyVetoException e) {
      log.error("Error iniatialization database pool", e);
    }
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

  private static QueryRunner newQueryRunner() {
    return new QueryRunner();
  }

  @SuppressWarnings("unused")
  public static List<BalanceSheet> getBalanceSheets() {
    try (Connection conn = getConnection()) {
      return getBalanceSheets(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static void clearDatabase() {
    try (Connection conn = getConnection()) {
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

  public static List<BalanceSheet> getBalanceSheets(Connection conn) throws SQLException {
    return new QueryRunner().query(conn, baseBsSelectQuery + "order by created_ts desc",
        new BeanListHandler<>(BalanceSheet.class));
  }

  public static BalanceSheet getBalanceSheet(UUID id) {
    try (Connection conn = getConnection()) {
      return getBalanceSheet(conn, id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private static BalanceSheet getBalanceSheet(Connection conn, UUID id) throws SQLException {
    return newQueryRunner().query(conn, baseBsSelectQuery + "where id = ?",
        new BeanHandler<>(BalanceSheet.class), id);
  }

  public static void createBalanceSheet(UUID id) {
    try (Connection conn = getConnection()) {
      createBalanceSheet(conn, id);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  static void createBalanceSheet(Connection conn, UUID id) throws SQLException {
    QueryRunner run = newQueryRunner();
    Timestamp now = new java.sql.Timestamp(new java.util.Date().getTime());
    String currencyCode = "RUB";
    run.update(conn, "insert into balance_sheets(id, created_ts, currency_code) values (?, ?, ?)", id,
        now, currencyCode);

    UUID svcRsvId = UUID.randomUUID();
    AccountsDao.createAccount(conn, id, new Account(svcRsvId, Account.Type.service, "Service reserve"));
    run.update(conn, "update balance_sheets set svc_rsv_id = ? where id = ?", svcRsvId, id);

    UUID uncatCostsId = UUID.randomUUID();
    AccountsDao.createAccount(conn, id, new Account(uncatCostsId, Account.Type.expense, "<Без категории>"));
    run.update(conn, "update balance_sheets set uncat_costs_id = ? where id = ?", uncatCostsId, id);

    UUID uncatIncomeId = UUID.randomUUID();
    AccountsDao.createAccount(conn, id, new Account(uncatIncomeId, Account.Type.income, "<Без категории>"));
    run.update(conn, "update balance_sheets set uncat_income_id = ? where id = ?", uncatIncomeId, id);

    BalancesDao.createBalance(conn, id,
        new Balance(UUID.randomUUID(), Account.Type.debit, "Наличные", currencyCode, BigDecimal.ZERO));
  }

  public static void deleteBalanceSheet(UUID id) {
    try (Connection conn = getConnection()) {
      QueryRunner run = newQueryRunner();
      run.update(conn, "delete from accounts where balance_sheet_id = ?", id);
      run.update(conn, "delete from balance_sheets where id = ?", id);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static BsStat getBsStat(UUID bsId, Long interval) {
    LocalDate today = LocalDate.now();
    java.sql.Date toDate = Date.valueOf(today);
    java.sql.Date fromDate = Date.valueOf(today.minusDays(interval));

    BsStat bsStat = new BsStat(bsId, fromDate, toDate);
    try (Connection conn = getConnection()) {
      QueryRunner run = newQueryRunner();
      BeanListHandler<Turnover> handler = new BeanListHandler<>(Turnover.class);

      calcCrntSaldo(conn, run, bsStat);

      TreeMap<Date, BsDayStat> map = new TreeMap<>();
      fillBsDayStatMap(map,
          getRealTurnovers(conn, run, handler, bsId, MoneyTrn.Status.done, fromDate, toDate));
      calcPastSaldoNTurnovers(bsStat, map);

      LocalDate trendFromLocalDate = today.plusDays(1).minusMonths(1);
      Date trendFromDate = Date.valueOf(trendFromLocalDate);
      Date trendToDate = Date.valueOf(trendFromLocalDate.plusDays(interval - 1));
      TreeMap<Date, BsDayStat> trendMap = new TreeMap<>();
      fillBsDayStatMap(trendMap,
          getTrendTurnovers(conn, run, handler, bsId, trendFromDate, trendToDate));
      fillBsDayStatMap(trendMap,
          getRealTurnovers(conn, run, handler, bsId, MoneyTrn.Status.pending,
              Date.valueOf(LocalDate.of(1970, 1, 1)), Date.valueOf(today.plusDays(interval))));
      fillBsDayStatMap(trendMap, getTemplTurnovers(bsId, Date.valueOf(today.plusDays(interval))));
      calcTrendSaldoNTurnovers(bsStat, trendMap);

      map.putAll(trendMap);
      bsStat.setDayStats(new ArrayList<>(map.values()));
      bsStat.setCategories(getCategoies(conn, run, bsId, fromDate, toDate));
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }

    return bsStat;
  }

  private static List<CategoryStat> getCategoies(Connection conn, QueryRunner run, UUID bsId, Date fromDate,
      Date toDate) throws SQLException {
    List<CategoryStat> list = run.query(conn, "" +
            "SELECT" +
            "  c.id," +
            "  c.root_id AS rootId," +
            "  a.name," +
            "  sum(t.amount) as amount " +
            "FROM" +
            "  accounts a," +
            "  categories c" +
            "    LEFT JOIN v_trns_by_base_crn t ON t.to_acc_id = c.id AND t.trn_date between ? AND ? " +
            "WHERE a.balance_sheet_id = ? " +
            "  AND a.id = c.id AND a.type = 'expense' AND t.status = 'done' " +
            "GROUP BY c.id, a.name, coalesce(c.root_id, c.id)",
        new BeanListHandler<>(CategoryStat.class), fromDate, toDate, bsId);
    list.stream()
        .filter(cs -> cs.getRootId() == null)
        .forEach(root -> {
           Double sum = list.stream()
               .filter(cs -> cs.getRootId() == root.getId())
               .mapToDouble(cs -> cs.getAmount().doubleValue())
               .reduce(0d, (s, d) -> s += d);
           root.setAmount(root.getAmount().add(new BigDecimal(sum)));
        });
    return list.stream()
        .filter(cs -> Objects.isNull(cs.getRootId()))
        .sorted((cs1, cs2) -> cs1.getAmount().compareTo(cs2.getAmount()) * -1)
        .collect(Collectors.toList());
  }

  /**
   * Вычисляет текущие балансы счетов и резервов.
   */
  private static void calcCrntSaldo(Connection conn, QueryRunner run, BsStat bsStat) throws SQLException {
    List<AggrAccSaldo> aggrAccSaldo = run.query(conn,
        "select type, sum(saldo) as saldo from v_crnt_saldo_by_base_cry where bs_id = ? group by type",
        new BeanListHandler<>(AggrAccSaldo.class), bsStat.getBsId());
    aggrAccSaldo.forEach(saldo -> bsStat.getSaldoMap().put(saldo.getType(), saldo.getSaldo()));
  }

  private static void calcPastSaldoNTurnovers(BsStat bsStat, Map<Date, BsDayStat> map) {
    Map<Account.Type, BigDecimal> saldoMap = new HashMap<>(Account.Type.values().length);
    bsStat.getSaldoMap().forEach((type, value) -> saldoMap.put(type, value.plus()));
    List<BsDayStat> dayStats = new ArrayList<>(map.values());
    dayStats.sort((e1, e2) -> (e1.getDateAsLocalDate().isAfter(e2.getDateAsLocalDate())) ? -1 : 1);
    dayStats.forEach(dayStat -> {
        Arrays.asList(Account.Type.values()).forEach(type -> {
          dayStat.setSaldo(type, saldoMap.getOrDefault(type, BigDecimal.ZERO));
          saldoMap.put(type, (saldoMap).getOrDefault(type, BigDecimal.ZERO).subtract(dayStat.getDelta(type)));
        });
        bsStat.setIncomeAmount(bsStat.getIncomeAmount().add(dayStat.getIncomeAmount()));
        bsStat.setChargesAmount(bsStat.getChargesAmount().add(dayStat.getChargeAmount()));
    });
  }

  private static void calcTrendSaldoNTurnovers(BsStat bsStat, Map<Date, BsDayStat> trendMap) {
    List<BsDayStat> dayStats = new ArrayList<>(trendMap.values());
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
      BsDayStat dayStat = map.computeIfAbsent(t.getTrnDate(), k -> new BsDayStat(t.getTrnDate().getTime()));
      dayStat.setDelta(t.getFromAccType(), dayStat.getDelta(t.getFromAccType()).subtract(t.getAmount()));
      dayStat.setDelta(t.getToAccType(), dayStat.getDelta(t.getToAccType()).add(t.getAmount()));
      if (Account.Type.income == t.getFromAccType()) {
        dayStat.setIncomeAmount(dayStat.getIncomeAmount().add(t.getAmount()));
      }
      if (Account.Type.expense == t.getToAccType()) {
        dayStat.setChargeAmount(dayStat.getChargeAmount().add(t.getAmount()));
      }
    });
  }

  private static List<Turnover> getRealTurnovers(Connection conn, QueryRunner run,
      ResultSetHandler<List<Turnover>> handler, UUID bsId, MoneyTrn.Status status, Date fromDate, Date toDate) {
    try {
      return run.query(conn,
          "select trn_date as trnDate, from_acc_type as fromAccType, to_acc_type as toAccType, " +
              "sum(amount) as amount " +
              "from v_trns_by_base_crn " +
              "where bs_id = ? and status = ? and trn_date between ? and ? " +
              "group by trn_date, from_acc_type, to_acc_type ",
          handler, bsId, status.name(), fromDate, toDate);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private static List<Turnover> getTrendTurnovers(Connection conn, QueryRunner run,
      ResultSetHandler<List<Turnover>> handler, UUID bsId, Date fromDate, Date toDate) {
    try {
      return run.query(conn,
          "select trn_date + interval '1 months' as trnDate, " +
              "from_acc_type as fromAccType, to_acc_type as toAccType, " +
              "sum(case when period = 'single' or not templ_id is null then 0 else amount end) as amount " +
              "from v_trns_by_base_crn " +
              "where bs_id = ? and status = ? and trn_date between ? and ? " +
              "group by trn_date, from_acc_type, to_acc_type ",
          handler, bsId, MoneyTrn.Status.done.name(), fromDate, toDate);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private static List<Turnover> getTemplTurnovers(UUID bsId, Date toDate) {
    List<MoneyTrnTempl> templs = MoneyTrnTemplsDao.getMoneyTrnTempls(bsId, "");
    Set<Turnover> turnovers = new HashSet<>();
    Date today = Date.valueOf(LocalDate.now());
    templs.forEach(t -> {
      Date templNextDate = t.getNextDate();
      while (templNextDate.compareTo(toDate) <= 0) {
        Account fromAcc = AccountsDao.getAccount(t.getFromAccId());
        Account toAcc = AccountsDao.getAccount(t.getToAccId());
        Date nextDate = (templNextDate.before(today)) ? today : templNextDate;
        Turnover newTurnover = new Turnover(nextDate, fromAcc.getType(), toAcc.getType());
        Optional<Turnover> turnover = turnovers.stream()
            .filter(t1 -> t1.equals(newTurnover))
            .findFirst();
        if (!turnover.isPresent()) {
          turnover = Optional.of(newTurnover);
          turnovers.add(newTurnover);
        }
        turnover.get().setAmount(turnover.get().getAmount().add(t.getAmount()));
        templNextDate = MoneyTrnTempl.calcNextDate(templNextDate, t.getPeriod());
      }
    });
    return new ArrayList<>(turnovers);
  }
}
