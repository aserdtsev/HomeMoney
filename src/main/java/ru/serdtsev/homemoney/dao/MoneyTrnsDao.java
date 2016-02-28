package ru.serdtsev.homemoney.dao;

import com.google.common.base.Strings;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import ru.serdtsev.homemoney.HmException;
import ru.serdtsev.homemoney.dto.*;

import javax.validation.constraints.NotNull;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

import static ru.serdtsev.homemoney.dto.MoneyTrn.Status;
import static ru.serdtsev.homemoney.dto.MoneyTrn.Status.done;
import static ru.serdtsev.homemoney.dto.MoneyTrn.Status.pending;

public class MoneyTrnsDao {
  private static final String baseSelect =
      "select mt.id, mt.status, mt.created_ts as createdTs, mt.trn_date as trnDate, mt.date_num as dateNum," +
          " mt.from_acc_id as fromAccId, fa.name as fromAccName, " +
          " mt.to_acc_id as toAccId, ta.name as toAccName, " +
          " case when fa.type = 'income' then 'income' when ta.type = 'expense' then 'expense' else 'transfer' end as type, " +
          " mt.parent_id as parentId, mt.amount, " +
          " mt.comment, mt.labels, mt.period, mt.templ_id as templId " +
        " from money_trns mt, accounts fa, accounts ta " +
        " where mt.balance_sheet_id = ? " +
          " and fa.id = mt.from_acc_id " +
          " and ta.id = mt.to_acc_id ";

  public static List<MoneyTrn> getDoneMoneyTrns(UUID bsId, String search, int limit, int offset) {
    List<MoneyTrn> trns;
    try (Connection conn = MainDao.getConnection()) {
      trns = getMoneyTrns(conn, bsId, Optional.of(MoneyTrn.Status.done), Optional.ofNullable(Strings.emptyToNull(search)),
          Optional.empty(), Optional.of(limit), Optional.of(offset));
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return trns;
  }

  public static List<MoneyTrn> getPendingMoneyTrns(UUID bsId, String search, Date beforeDate) {
    List<MoneyTrn> trns;
    try (Connection conn = MainDao.getConnection()) {
      trns = getMoneyTrns(conn, bsId, Optional.of(MoneyTrn.Status.pending), Optional.ofNullable(search), Optional.of(beforeDate),
          Optional.empty(), Optional.empty());
      trns.addAll(getTemplMoneyTrns(conn, bsId, Optional.ofNullable(Strings.emptyToNull(search)), beforeDate));
      Collections.sort(trns, (t1, t2) -> (t2.getTrnDate().compareTo(t1.getTrnDate())));
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return trns;
  }

  public static List<MoneyTrn> getMoneyTrns(Connection conn, UUID bsId, Optional<Status> status,
      Optional<String> search, Optional<Date> beforeDate, Optional<Integer> limit, Optional<Integer> offset) {
    List<MoneyTrn> moneyTrnList;
    try {
      ResultSetHandler<List<MoneyTrn>> mtHandler = new BeanListHandler<>(MoneyTrn.class,
          new BasicRowProcessor((new MoneyTrnProcessor())));
      QueryRunner run = new QueryRunner();

      final StringBuilder sql = new StringBuilder(baseSelect);
      final List<Object> params = new ArrayList<>();
      params.add(bsId);

      status.ifPresent(s -> {
        sql.append(" and status = ? ");
        params.add(s.name());
      });

      beforeDate.ifPresent(d -> {
        sql.append(" and mt.trn_date < ? ");
        params.add(d);
      });

      search.ifPresent(s -> {
        sql.append(" and (mt.comment ilike ? or mt.labels ilike ? or fa.name ilike ? or ta.name ilike ?)");
        final String searchTempl = "%" + s + "%";
        for (int i = 0; i < 4; i++) {
          params.add(searchTempl);
        }
      });

      sql.append(" order by trn_date desc, date_num, created_ts desc ");

      limit.ifPresent(l -> {
        sql.append(" limit ? ");
        params.add(l);
      });

      offset.ifPresent(o -> {
        sql.append(" offset ? ");
        params.add(o);
      });

      moneyTrnList = run.query(conn, sql.toString(), mtHandler, params.toArray());
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return moneyTrnList;
  }

  public static List<MoneyTrn> getMoneyTrns(Connection conn, UUID bsId, Date trnDate) {
    List<MoneyTrn> moneyTrnList;
    try {
      ResultSetHandler<List<MoneyTrn>> mtHandler = new BeanListHandler<>(MoneyTrn.class,
          new BasicRowProcessor((new MoneyTrnProcessor())));
      QueryRunner run = new QueryRunner();

      moneyTrnList = run.query(conn,
          baseSelect  + " and mt.trn_date = ?" +
              " order by date_num, created_ts desc",
          mtHandler, bsId, trnDate);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return moneyTrnList;
  }

  private static List<MoneyTrn> getTemplMoneyTrns(Connection conn, UUID bsId, Optional<String> search, Date beforeDate) {
    List<MoneyTrn> list;
    try {
      ResultSetHandler<List<MoneyTrn>> handler = new BeanListHandler<>(MoneyTrn.class,
          new BasicRowProcessor((new MoneyTrnProcessor())));
      QueryRunner run = new QueryRunner();
      final StringBuilder sql = new StringBuilder(
          "select null as id, 'pending' as status, null as createdTs, te.next_date as trnDate, 0 as dateNum, " +
              "    tr.from_acc_id as fromAccId, fa.name as fromAccName, " +
              "    tr.to_acc_id as toAccId, ta.name as toAccName, " +
              "    case when fa.type = 'income' then 'income' when ta.type = 'expense' then 'expense' else 'transfer' end as type, " +
              "    null as parentId, te.amount, tr.comment, tr.labels, " +
              "    tr.period, te.id as templId " +
              "  from money_trn_templs te, money_trns tr, accounts fa, accounts ta " +
              "  where te.bs_id = ? and te.status = 'active' " +
              "    and tr.id = te.sample_id and te.next_date < ? " +
              "    and fa.id = tr.from_acc_id and ta.id = tr.to_acc_id ");
      final List<Object> params = new ArrayList<>();
      params.add(bsId);
      params.add(beforeDate);
      search.ifPresent(s -> {
        sql.append(" and (te.comment ilike ? or te.labels ilike ? or fa.name ilike ? or ta.name ilike ?)");
        final String searchTempl = "%" + s + "%";
        for (int i = 0; i < 4; i++) {
          params.add(searchTempl);
        }
      });
      sql.append(" order by trnDate desc ");
      list = run.query(conn, sql.toString(), handler, params.toArray());
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return list;
  }

  public static MoneyTrn getMoneyTrn(UUID bsId, UUID id) {
    try (Connection conn = MainDao.getConnection()) {
      return getMoneyTrn(conn, bsId, id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static MoneyTrn getMoneyTrn(Connection conn, UUID bsId, UUID id) {
    MoneyTrn moneyTrn;
    try {
      ResultSetHandler<MoneyTrn> mtHandler = new BeanHandler<>(MoneyTrn.class,
          new BasicRowProcessor((new MoneyTrnProcessor())));
      QueryRunner run = new QueryRunner();

      moneyTrn = run.query(conn,
          baseSelect + " and mt.id = ?",
          mtHandler, bsId, id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return moneyTrn;
  }

  public static MoneyTrn getChildMoneyTrn(Connection conn, UUID bsId, UUID id) {
    MoneyTrn moneyTrn;
    try {
      ResultSetHandler<MoneyTrn> mtHandler = new BeanHandler<>(MoneyTrn.class,
          new BasicRowProcessor((new MoneyTrnProcessor())));
      QueryRunner run = new QueryRunner();

      moneyTrn = run.query(conn,
          baseSelect + " and mt.parent_id = ?",
          mtHandler, bsId, id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return moneyTrn;
  }

  public static List<MoneyTrn> createMoneyTrn(UUID bsId, MoneyTrn moneyTrn) {
    List<MoneyTrn> result = new ArrayList<>();
    try (Connection conn = MainDao.getConnection()) {
      createMoneyTrn(conn, bsId, moneyTrn, result);
      if (moneyTrn.getTemplId() != null) {
        MoneyTrnTempl templ = MoneyTrnTemplsDao.getMoneyTrnTempl(conn, bsId, moneyTrn.getTemplId());
        templ.setNextDate(MoneyTrnTempl.Companion.calcNextDate(templ.getNextDate(), templ.getPeriod()));
        MoneyTrnTemplsDao.updateMoneyTrnTempl(bsId, templ);
      }
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return result;
  }

  public static void createMoneyTrn(Connection conn, UUID bsId, MoneyTrn moneyTrn)
      throws SQLException {
    createMoneyTrn(conn, bsId, moneyTrn, new ArrayList<>(0));
  }

  private static void createMoneyTrn(Connection conn, UUID bsId, MoneyTrn moneyTrn,
      List<MoneyTrn> result) throws SQLException {
    createMoneyTrnInternal(conn, bsId, moneyTrn);
    MoneyTrn trn1 = getMoneyTrn(conn, bsId, moneyTrn.getId());
    if (result != null) {
      result.add(trn1);
    }
    MoneyTrn trn2 = createReserveMoneyTrn(conn, bsId, moneyTrn);
    if (trn2 != null && result != null) {
      result.add(trn2);
    }
    if (done.equals(moneyTrn.getStatus())
        && !moneyTrn.getTrnDate().toLocalDate().isAfter(LocalDate.now())) {
      result.forEach(t -> completeMoneyTrn(conn, bsId, t.getId()));
    }
  }

  private static MoneyTrn createReserveMoneyTrn(Connection conn, UUID balanceSheetId, MoneyTrn moneyTrn)
      throws SQLException {
    BalanceSheet bs = MainDao.getBalanceSheet(balanceSheetId);
    Account svcRsv = AccountsDao.getAccount(bs.svcRsvId);

    Account fromAcc = svcRsv;
    Account account = AccountsDao.getAccount(moneyTrn.getFromAccId());
    if (Account.Type.debit.equals(account.getType())) {
      Balance balance = BalancesDao.getBalance(conn, account.getId());
      if (balance.getReserveId() != null) {
        fromAcc = ReservesDao.getReserve(balance.getReserveId());
      }
    }

    Account toAcc = svcRsv;
    account = AccountsDao.getAccount(moneyTrn.getToAccId());
    if (Account.Type.debit.equals(account.getType())) {
      Balance balance = BalancesDao.getBalance(conn, account.getId());
      if (balance.getReserveId() != null) {
        toAcc = ReservesDao.getReserve(balance.getReserveId());
      }
    }

    if (!fromAcc.equals(toAcc)) {
      MoneyTrn rMoneyTrn = new MoneyTrn(UUID.randomUUID(), moneyTrn.getStatus(), moneyTrn.getTrnDate(),
          moneyTrn.getDateNum(), fromAcc.getId(), toAcc.getId(), moneyTrn.getId(), moneyTrn.getAmount(),
          moneyTrn.getComment(), moneyTrn.getCreatedTs(), moneyTrn.getPeriod(), moneyTrn.getLabels(), null);
      createMoneyTrnInternal(conn, balanceSheetId, rMoneyTrn);
      return getMoneyTrn(conn, balanceSheetId, rMoneyTrn.getId());
    }
    return null;
  }

  public static void createMoneyTrnInternal(Connection conn, UUID balanceSheetId, MoneyTrn moneyTrn) throws SQLException {
    QueryRunner run = new QueryRunner();
    java.sql.Timestamp createdTs = new java.sql.Timestamp(new java.util.Date().getTime());

    run.update(conn,
        "insert into money_trns(id, status, balance_sheet_id, created_ts, trn_date, date_num, " +
            "from_acc_id, to_acc_id, parent_id, amount, comment, labels, period, templ_id) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        moneyTrn.getId(),
        pending.name(),
        balanceSheetId,
        createdTs,
        moneyTrn.getTrnDate(),
        0,
        moneyTrn.getFromAccId(),
        moneyTrn.getToAccId(),
        moneyTrn.getParentId(),
        moneyTrn.getAmount(),
        moneyTrn.getComment(),
        moneyTrn.getLabelsAsString(),
        moneyTrn.getPeriod().name(),
        moneyTrn.getTemplId());
  }

  private static void completeMoneyTrn(Connection conn, UUID bsId, UUID moneyTrnId) {
    try {
      setStatus(conn, bsId, moneyTrnId, done);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private static void setStatus(Connection conn, UUID bsId, UUID moneyTrnId, @NotNull Status status) throws SQLException {
    MoneyTrn trn = getMoneyTrn(conn, bsId, moneyTrnId);
    if (status.equals(trn.getStatus())) {
      return;
    }
    Optional<UUID> fromAccId = Optional.empty();
    Optional<UUID> toAccId = Optional.empty();
    switch (status) {
      case done:
        if (trn.getStatus().equals(Status.pending) || trn.getStatus().equals(Status.cancelled)) {
          fromAccId = Optional.of(trn.getFromAccId());
          toAccId = Optional.of(trn.getToAccId());
        }
        break;
      case cancelled: case pending:
        if (trn.getStatus().equals(Status.done)) {
          fromAccId = Optional.of(trn.getToAccId());
          toAccId = Optional.of(trn.getFromAccId());
        }
        break;
      default:
        throw new HmException(HmException.Code.UnknownMoneyTrnStatus);
    }

    fromAccId.ifPresent(accId -> {
      Account fromAccount = AccountsDao.getAccount(accId);
      if (!trn.getTrnDate().before(fromAccount.getCreatedDate()) && fromAccount.isBalance()) {
        Balance fromBalance;
        try {
          fromBalance = BalancesDao.getBalance(conn, accId);
          BalancesDao.changeBalanceValue(conn, fromBalance, trn.getAmount().negate());
        } catch (SQLException e) {
          throw new HmSqlException(e);
        }
      }
    });

    toAccId.ifPresent(accId -> {
      Account toAccount = AccountsDao.getAccount(accId);
      if (!trn.getTrnDate().before(toAccount.getCreatedDate()) && toAccount.isBalance()) {
        Balance toBalance;
        try {
          toBalance = BalancesDao.getBalance(conn, accId);
          BalancesDao.changeBalanceValue(conn, toBalance, trn.getAmount());
        } catch (SQLException e) {
          throw new HmSqlException(e);
        }
      }
    });

    QueryRunner run = new QueryRunner();
    run.update(conn, "update money_trns set status = ? where id = ?",
        status.name(), trn.getId());
  }

  public static void deleteMoneyTrn(UUID bsId, UUID id) {
    try (Connection conn = MainDao.getConnection()) {
      setStatus(conn, bsId, id, Status.cancelled);
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

  private static void updateMoneyTrn(Connection conn, UUID bsId, MoneyTrn trn)
      throws SQLException {
    QueryRunner run = new QueryRunner();
    MoneyTrn origTrn = getMoneyTrn(conn, bsId, trn.getId());
    if (trn.crucialEquals(origTrn)) {
      run.update(conn,
          "update money_trns set date_num = ?, period = ?, comment = ?, labels = ? where id = ?",
          trn.getDateNum(), trn.getPeriod().name(), trn.getComment(), trn.getLabelsAsString(), trn.getId());
    } else {
      Status origTrnStatus = trn.getStatus();
      setStatus(conn, bsId, trn.getId(), Status.cancelled);
      run.update(conn,
          "update money_trns set " +
              " trn_date = ?," +
              " date_num = ?," +
              " from_acc_id = ?," +
              " to_acc_id = ?," +
              " amount = ?," +
              " period = ?, " +
              " comment = ?, " +
              " labels = ? " +
              " where id = ?",
          trn.getTrnDate(),
          trn.getDateNum(),
          trn.getFromAccId(),
          trn.getToAccId(),
          trn.getAmount(),
          trn.getPeriod().name(),
          trn.getComment(),
          trn.getLabelsAsString(),
          trn.getId());
      setStatus(conn, bsId, trn.getId(), origTrnStatus);
    }
  }

  public static void skipMoneyTrn(UUID bsId, MoneyTrn trn) {
    try (Connection conn = MainDao.getConnection()) {
      if (trn.getId() != null) {
        setStatus(conn, bsId, trn.getId(), Status.cancelled);
      }
      if (trn.getTemplId() != null) {
        MoneyTrnTempl templ = MoneyTrnTemplsDao.getMoneyTrnTempl(conn, bsId, trn.getTemplId());
        templ.setNextDate(MoneyTrnTempl.Companion.calcNextDate(templ.getNextDate(), templ.getPeriod()));
        MoneyTrnTemplsDao.updateMoneyTrnTempl(bsId, templ);
      }
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
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
        QueryRunner run = new QueryRunner();
        int i = 0;
        for (MoneyTrn m : list) {
          run.update(conn,
              "update money_trns set date_num = ? " +
                  "where balance_sheet_id = ? and id = ?",
              i, bsId, m.getId());
          i++;
        }
      }
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }
}
