package ru.serdtsev.homemoney.dao;

import com.google.common.base.Strings;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import ru.serdtsev.homemoney.HmException;
import ru.serdtsev.homemoney.dto.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.serdtsev.homemoney.dto.MoneyTrn.Status.done;
import static ru.serdtsev.homemoney.dto.MoneyTrn.Status.pending;

public class MoneyTrnsDao {
  private static final String baseSelect = "" +
      "select mt.id, mt.status, mt.created_ts as createdTs, mt.trn_date as trnDate, mt.date_num as dateNum," +
      "   mt.from_acc_id as fromAccId, fa.name as fromAccName, " +
      "   mt.to_acc_id as toAccId, ta.name as toAccName, " +
      "   case when fa.type = 'income' then 'income' when ta.type = 'expense' then 'expense' else 'transfer' end as type, " +
      "   mt.parent_id as parentId, " +
      "   mt.amount, coalesce(coalesce(fb.currency_code, tb.currency_code), 'RUB') as currencyCode, " +
      "   coalesce(mt.to_amount, mt.amount) as toAmount, coalesce(coalesce(tb.currency_code, fb.currency_code), 'RUB') toCurrencyCode, " +
      "   mt.comment, mt.period, mt.templ_id as templId " +
      " from money_trns mt, " +
      "   accounts fa " +
      "     left join balances fb on fb.id = fa.id, " +
      "   accounts ta" +
      "     left join balances tb on tb.id = ta.id " +
      " where mt.balance_sheet_id = ? " +
      "   and fa.id = mt.from_acc_id " +
      "   and ta.id = mt.to_acc_id ";


  public static List<MoneyTrn> getDoneMoneyTrns(UUID bsId, String search, Integer limit, Integer offset) {
    List<MoneyTrn> trns;
    try (Connection conn = MainDao.getConnection()) {
      trns = getMoneyTrns(conn, bsId, done, search, limit, offset, null);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return trns;
  }

  public static List<MoneyTrn> getPendingMoneyTrns(UUID bsId, String search, Date beforeDate) {
    List<MoneyTrn> trns;
    try (Connection conn = MainDao.getConnection()) {
      trns = getMoneyTrns(conn, bsId, pending, search, null, null, beforeDate);
      trns.addAll(getTemplMoneyTrns(conn, bsId, search, beforeDate));
      trns.sort((t1, t2) -> t2.getTrnDate().compareTo(t1.getTrnDate()));
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return trns;
  }

  private static List<MoneyTrn> getMoneyTrns(Connection conn, UUID bsId, MoneyTrn.Status status, String search,
      Integer limit, Integer offset, Date beforeDate) throws SQLException {
    List<MoneyTrn> moneyTrnList;

    StringBuilder sql = new StringBuilder(baseSelect);
    List<Object> params = new ArrayList<>();
    params.add(bsId);

    sql.append(" and status = ? ");
    params.add(status.name());

    if (beforeDate != null) {
      sql.append(" and mt.trn_date < ? ");
      params.add(beforeDate);
    }

    if (!Strings.isNullOrEmpty(search)) {
      String condition = " and (mt.comment ilike ? or fa.name ilike ? or ta.name ilike ? " +
          " or exists (select null from labels2objs l2o, labels l where l2o.obj_id = mt.id and l.id = l2o.label_id and l.name ilike ?)) ";
      sql.append(condition);
      long paramNum = condition.chars().filter(ch -> ch == '?').count();
      IntStream.range(0, ((int) paramNum)).forEach(i -> params.add("%" + search + "%"));
    }

    sql.append(" order by trn_date desc, date_num, created_ts desc ");

    if (limit != null) {
      sql.append(" limit ? ");
      params.add(limit);
    }

    if (offset != null) {
      sql.append(" offset ? ");
      params.add(offset);
    }

    moneyTrnList = new QueryRunner().query(conn, sql.toString(),
        new BeanListHandler<>(MoneyTrn.class, new BasicRowProcessor(new MoneyTrnProcessor())),
        params.toArray()).stream().peek(trn -> {
      try {
        trn.setLabels(LabelsDao.getLabelNames(conn, trn.getId()));
      } catch (SQLException e) {
        throw new HmSqlException(e);
      }
    }).collect(Collectors.toList());

    return moneyTrnList;
  }

  private static List<MoneyTrn> getMoneyTrns(Connection conn, UUID bsId, Date trnDate) throws SQLException {
    return new QueryRunner().query(conn,
        baseSelect + " and mt.trn_date = ?" +
            " order by date_num, created_ts desc",
        new BeanListHandler<>(MoneyTrn.class, new BasicRowProcessor(new MoneyTrnProcessor())), bsId, trnDate)
        .stream()
        .peek(t -> {
          try {
            t.setLabels(LabelsDao.getLabelNames(conn, t.getId()));
          } catch (SQLException e) {
            e.printStackTrace();
          }
        })
        .collect(Collectors.toList());
  }

  private static List<MoneyTrn> getTemplMoneyTrns(Connection conn, UUID bsId, String search, Date beforeDate) throws SQLException {
    BeanListHandler<MoneyTrn> handler = new BeanListHandler<>(MoneyTrn.class,
        new BasicRowProcessor(new MoneyTrnProcessor()));
    StringBuilder sql = new StringBuilder("" +
        "select null as id, 'pending' as status, null as createdTs, te.next_date as trnDate, 0 as dateNum, " +
        "    tr.from_acc_id as fromAccId, fa.name as fromAccName, " +
        "    tr.to_acc_id as toAccId, ta.name as toAccName, " +
        "    case when fa.type = 'income' then 'income' when ta.type = 'expense' then 'expense' else 'transfer' end as type, " +
        "    null as parentId, te.amount, coalesce(coalesce(fb.currency_code, tb.currency_code), 'RUB') as currencyCode, " +
        "    te.to_amount as toAmount, coalesce(coalesce(tb.currency_code, fb.currency_code), 'RUB') as toCurrencyCode, " +
        "    tr.comment, tr.period, te.id as templId " +
        "  from money_trn_templs te, money_trns tr, " +
        "    accounts fa " +
        "      left join balances fb on fb.id = fa.id, " +
        "    accounts ta " +
        "      left join balances tb on tb.id = ta.id " +
        "  where te.bs_id = ? and te.status = 'active' " +
        "    and tr.id = te.sample_id and te.next_date < ? " +
        "    and fa.id = tr.from_acc_id and ta.id = tr.to_acc_id ");
    List<Object> params = new ArrayList<>();
    params.add(bsId);
    params.add(beforeDate);
    if (!Strings.isNullOrEmpty(search)) {
      String condition = " and (te.comment ilike ? or fa.name ilike ? or ta.name ilike ? " +
          " or exists (select null from labels2objs l2o, labels l where l2o.obj_id = te.id and l.id = l2o.label_id and l.name ilike ?)) ";
      sql.append(condition);
      long paramNum = condition.chars().filter(ch -> ch == '?').count();
      IntStream.range(0, ((int) paramNum)).forEach(i -> params.add("%" + search + "%"));
    }
    sql.append(" order by trnDate desc ");
    return new QueryRunner()
        .query(conn, sql.toString(), handler, params.toArray())
        .stream()
        .peek(templ -> {
          try {
            templ.setLabels(LabelsDao.getLabelNames(conn, templ.getTemplId()));
          } catch (SQLException e) {
            throw new HmSqlException(e);
          }
        })
        .collect(Collectors.toList());
  }

  public static MoneyTrn getMoneyTrn(UUID bsId, UUID id) {
    try (Connection conn = MainDao.getConnection()) {
      return getMoneyTrn(conn, bsId, id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private static MoneyTrn getMoneyTrn(Connection conn, UUID bsId, UUID id) throws SQLException {
    BeanHandler<MoneyTrn> mtHandler = new BeanHandler<>(MoneyTrn.class, new BasicRowProcessor(new MoneyTrnProcessor()));
    MoneyTrn trn = new QueryRunner().query(conn, baseSelect + " and mt.id = ?", mtHandler, bsId, id);
    trn.setLabels(LabelsDao.getLabelNames(conn, id));
    return trn;
  }

  public static List<MoneyTrn> createMoneyTrn(UUID bsId, MoneyTrn moneyTrn) {
    List<MoneyTrn> result = new ArrayList<>();
    try (Connection conn = MainDao.getConnection()) {
      createMoneyTrn(conn, bsId, moneyTrn, result);
      if (moneyTrn.getTemplId() != null) {
        MoneyTrnTempl templ = MoneyTrnTemplsDao.getMoneyTrnTempl(conn, bsId, moneyTrn.getTemplId());
        templ.setNextDate(MoneyTrnTempl.calcNextDate(templ.getNextDate(), templ.getPeriod()));
        MoneyTrnTemplsDao.updateMoneyTrnTempl(bsId, templ);
      }
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return result;
  }

  static void createMoneyTrn(Connection conn, UUID bsId, MoneyTrn moneyTrn) throws SQLException {
    createMoneyTrn(conn, bsId, moneyTrn, new ArrayList<>(0));
  }

  private static void createMoneyTrn(Connection conn, UUID bsId, MoneyTrn moneyTrn, List<MoneyTrn> result)
      throws SQLException {
    Objects.requireNonNull(result);
    createMoneyTrnInternal(conn, bsId, moneyTrn);
    MoneyTrn trn1 = getMoneyTrn(conn, bsId, moneyTrn.getId());
    result.add(trn1);
    MoneyTrn trn2 = createReserveMoneyTrn(conn, bsId, moneyTrn);
    if (trn2 != null) {
      result.add(trn2);
    }
    if (done == moneyTrn.getStatus() && !moneyTrn.getTrnDate().toLocalDate().isAfter(LocalDate.now())) {
      result.forEach(t -> {
        try {
          completeMoneyTrn(conn, bsId, t.getId());
        } catch (SQLException e) {
          throw new HmSqlException(e);
        }
      });
    }
  }

  private static MoneyTrn createReserveMoneyTrn(Connection conn, UUID balanceSheetId, MoneyTrn moneyTrn) throws SQLException {
    BalanceSheet bs = MainDao.getBalanceSheet(balanceSheetId);
    Account svcRsv = AccountsDao.getAccount(bs.getSvcRsvId());

    Account fromAcc = svcRsv;
    Account account = AccountsDao.getAccount(moneyTrn.getFromAccId());
    if (Account.Type.debit == account.getType()) {
      Balance balance = BalancesDao.getBalance(conn, account.getId());
      if (balance.getReserveId() != null) {
        fromAcc = ReservesDao.getReserve(balance.getReserveId());
      }
    }

    Account toAcc = svcRsv;
    account = AccountsDao.getAccount(moneyTrn.getToAccId());
    if (Account.Type.debit == account.getType()) {
      Balance balance = BalancesDao.getBalance(conn, account.getId());
      if (balance.getReserveId() != null) {
        toAcc = ReservesDao.getReserve(balance.getReserveId());
      }
    }

    if (fromAcc != toAcc) {
      MoneyTrn rMoneyTrn = new MoneyTrn(UUID.randomUUID(), moneyTrn.getStatus(), moneyTrn.getTrnDate(),
          fromAcc.getId(), toAcc.getId(), moneyTrn.getAmount(), moneyTrn.getPeriod(), moneyTrn.getComment(), moneyTrn.getLabels(),
          moneyTrn.getDateNum(), moneyTrn.getId(), null, moneyTrn.getCreatedTs());
      createMoneyTrnInternal(conn, balanceSheetId, rMoneyTrn);
      return getMoneyTrn(conn, balanceSheetId, rMoneyTrn.getId());
    }
    return null;
  }

  private static void createMoneyTrnInternal(Connection conn, UUID bsId, MoneyTrn moneyTrn) throws SQLException {
    Timestamp createdTs = new Timestamp(new java.util.Date().getTime());

    QueryRunner run = new QueryRunner();
    run.update(conn, "" +
            "insert into money_trns(id, status, balance_sheet_id, created_ts, trn_date, date_num, " +
            "    from_acc_id, to_acc_id, parent_id, amount, to_amount, comment, period, templ_id) " +
            "  values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        moneyTrn.getId(),
        pending.name(),
        bsId,
        createdTs,
        moneyTrn.getTrnDate(),
        0,
        moneyTrn.getFromAccId(),
        moneyTrn.getToAccId(),
        moneyTrn.getParentId(),
        moneyTrn.getAmount(),
        moneyTrn.getToAmount(),
        moneyTrn.getComment(),
        moneyTrn.getPeriod().name(),
        moneyTrn.getTemplId());
     LabelsDao.saveLabels(conn, bsId, moneyTrn.getId(), "operation", moneyTrn.getLabels());
  }

  private static void completeMoneyTrn(Connection conn, UUID bsId, UUID moneyTrnId) throws SQLException {
    setStatusNChangeBalanceValues(conn, bsId, moneyTrnId, done);
  }

  private static void setStatusNChangeBalanceValues(Connection conn, UUID bsId, UUID moneyTrnId, MoneyTrn.Status status)
      throws SQLException {
    MoneyTrn trn = getMoneyTrn(conn, bsId, moneyTrnId);
    if (status == trn.getStatus()) return;
    UUID fromAccId = null;
    BigDecimal fromAmount = null;
    UUID toAccId = null;
    BigDecimal toAmount = null;
    switch (status) {
      case done:
        if (trn.getStatus() == MoneyTrn.Status.pending || trn.getStatus() == MoneyTrn.Status.cancelled) {
          fromAccId = trn.getFromAccId();
          fromAmount = trn.getAmount();
          toAccId = trn.getToAccId();
          toAmount = trn.getToAmount();
        }
        break;
      case cancelled: case pending:
        if (trn.getStatus() == MoneyTrn.Status.done) {
          fromAccId = trn.getToAccId();
          fromAmount = trn.getToAmount();
          toAccId = trn.getFromAccId();
          toAmount = trn.getAmount();
        }
        break;
      default:
        throw new HmException(HmException.Code.UnknownMoneyTrnStatus);
    }

    if (fromAccId != null) {
      Account fromAccount = AccountsDao.getAccount(fromAccId);
      if (!trn.getTrnDate().before(fromAccount.getCreatedDate()) && fromAccount.isBalance()) {
        Balance fromBalance = BalancesDao.getBalance(conn, fromAccId);
        BalancesDao.changeBalanceValue(conn, fromBalance, fromAmount.negate(), trn.getId(), status);
      }
    }

    if (toAccId != null) {
      Account toAccount = AccountsDao.getAccount(toAccId);
      if (!trn.getTrnDate().before(toAccount.getCreatedDate()) && toAccount.isBalance()) {
        Balance toBalance = BalancesDao.getBalance(conn, toAccId);
        BalancesDao.changeBalanceValue(conn, toBalance, toAmount, trn.getId(), status);
      }
    }

    new QueryRunner().update(conn, "update money_trns set status = ? where id = ?", status.name(), trn.getId());
  }

  public static void deleteMoneyTrn(UUID bsId, UUID id) {
    try (Connection conn = MainDao.getConnection()) {
      setStatusNChangeBalanceValues(conn, bsId, id);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static void updateMoneyTrn(UUID bsId, MoneyTrn moneyTrn) {
    try (Connection conn = MainDao.getConnection()) {
      updateMoneyTrn(conn, bsId, moneyTrn);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private static void updateMoneyTrn(Connection conn, UUID bsId, MoneyTrn trn) throws SQLException {
    if (trn.isMonoCurrencies() && !Objects.equals(trn.getAmount(), trn.getToAmount()))
      throw new HmException(HmException.Code.WrongAmount);
    QueryRunner run = new QueryRunner();
    MoneyTrn origTrn = getMoneyTrn(conn, bsId, trn.getId());
    if (trn.essentialEquals(origTrn)) {
      run.update(conn,
          "update money_trns set date_num = ?, period = ?, comment = ? where id = ?",
          trn.getDateNum(), trn.getPeriod().name(), trn.getComment(), trn.getId());
      LabelsDao.saveLabels(conn, bsId, trn.getId(), "operation", trn.getLabels());
    } else {
      MoneyTrn.Status origTrnStatus = trn.getStatus();
      setStatusNChangeBalanceValues(conn, bsId, trn.getId(), MoneyTrn.Status.cancelled);
      run.update(conn, "" +
              "update money_trns set " +
              "    trn_date = ?," +
              "    date_num = ?," +
              "    from_acc_id = ?," +
              "    to_acc_id = ?," +
              "    amount = ?," +
              "    to_amount = ?," +
              "    period = ?, " +
              "    comment = ? " +
              "  where id = ?",
          trn.getTrnDate(),
          trn.getDateNum(),
          trn.getFromAccId(),
          trn.getToAccId(),
          trn.getAmount(),
          trn.isMonoCurrencies() ? null : trn.getToAmount(),
          trn.getPeriod().name(),
          trn.getComment(),
          trn.getId());
      LabelsDao.saveLabels(conn, bsId, trn.getId(), "operation", trn.getLabels());
      setStatusNChangeBalanceValues(conn, bsId, trn.getId(), origTrnStatus);
    }
  }

  public static void skipMoneyTrn(UUID bsId, MoneyTrn trn) {
    try (Connection conn = MainDao.getConnection()) {
      if (trn.getId() != null) {
        setStatusNChangeBalanceValues(conn, bsId, trn.getId(), MoneyTrn.Status.cancelled);
      }
      if (trn.getTemplId() != null) {
        MoneyTrnTempl templ = MoneyTrnTemplsDao.getMoneyTrnTempl(conn, bsId, trn.getTemplId());
        templ.setNextDate(MoneyTrnTempl.calcNextDate(templ.getNextDate(), templ.getPeriod()));
        MoneyTrnTemplsDao.updateMoneyTrnTempl(bsId, templ);
      }
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private static void setStatusNChangeBalanceValues(Connection conn, UUID bsId, UUID id) throws SQLException {
    setStatusNChangeBalanceValues(conn, bsId, id, MoneyTrn.Status.cancelled);
  }

  public static void upMoneyTrn(UUID bsId, UUID id) {
    try (Connection conn = MainDao.getConnection()) {
      MoneyTrn moneyTrn = getMoneyTrn(conn, bsId, id);
      List<MoneyTrn> list = getMoneyTrns(conn, bsId, moneyTrn.getTrnDate());
      int index = list.indexOf(moneyTrn);
      if (index > 0) {
        MoneyTrn prevMoneyTrn = list.get(index - 1);
        list.set(index - 1, moneyTrn);
        list.set(index, prevMoneyTrn);
        for (int i = 0; i < list.size(); i++) {
          new QueryRunner().update(conn,
              "update money_trns set date_num = ? " + "where balance_sheet_id = ? and id = ?",
              i, bsId, list.get(i).getId());
        }
      }
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }

  }
}
