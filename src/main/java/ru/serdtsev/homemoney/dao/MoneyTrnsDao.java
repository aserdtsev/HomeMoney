package ru.serdtsev.homemoney.dao;

import com.google.common.base.Strings;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.serdtsev.homemoney.HmException;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.dto.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.serdtsev.homemoney.dto.MoneyTrn.Status.done;
import static ru.serdtsev.homemoney.dto.MoneyTrn.Status.doneNew;
import static ru.serdtsev.homemoney.dto.MoneyTrn.Status.pending;
import static ru.serdtsev.homemoney.utils.Utils.assertNonNulls;
import static ru.serdtsev.homemoney.utils.Utils.nvl;

@Component
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

  private BalancesDao balancesDao;
  private ReservesDao reservesDao;
  private BalanceSheetRepository balanceSheetRepo;

  @Autowired
  public MoneyTrnsDao(BalancesDao balancesDao, ReservesDao reservesDao, BalanceSheetRepository balanceSheetRepo) {
    this.balancesDao = balancesDao;
    this.reservesDao = reservesDao;
    this.balanceSheetRepo = balanceSheetRepo;
  }

  @NotNull
  public List<MoneyTrn> getDoneMoneyTrns(UUID bsId, @Nullable String search, @Nullable Integer limit,
      @Nullable Integer offset) {
    assertNonNulls(bsId);
    List<MoneyTrn> trns;
    try (Connection conn = MainDao.getConnection()) {
      trns = getMoneyTrns(conn, bsId, done, search, limit, offset, null);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return trns;
  }

  @NotNull
  public List<MoneyTrn> getPendingAndRecurrenceMoneyTrns(UUID bsId, String search, Date beforeDate) {
    List<MoneyTrn> trns;
    try (Connection conn = MainDao.getConnection()) {
      // todo Применить CallableFuture.
      trns = getMoneyTrns(conn, bsId, pending, search, null, null, beforeDate);
      trns.addAll(getTemplMoneyTrns(conn, bsId, search, beforeDate));
      trns.sort((t1, t2) -> t2.getTrnDate().compareTo(t1.getTrnDate()));
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return trns;
  }

  @NotNull
  List<MoneyTrn> getMoneyTrns(Connection conn, UUID bsId) throws SQLException {
    assertNonNulls(conn, bsId);
    return new QueryRunner().query(conn, baseSelect,
        new BeanListHandler<>(MoneyTrn.class, new BasicRowProcessor(new MoneyTrnProcessor())), bsId)
        .stream().peek(trn -> {
      try {
        trn.setBalanceChanges(getBalanceChanges(conn, trn.getId()));
        trn.setLabels(LabelsDao.getLabelNames(conn, trn.getId()));
      } catch (SQLException e) {
        throw new HmSqlException(e);
      }
    }).collect(Collectors.toList());
  }

  @NotNull
  private List<MoneyTrn> getMoneyTrns(Connection conn, UUID bsId, MoneyTrn.Status status,
      @Nullable String search, @Nullable Integer limit, @Nullable Integer offset, @Nullable Date beforeDate) throws SQLException {
    assertNonNulls(conn, bsId, status);

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
      if (search.matches("\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}")) {
        // Ищем по идентификатору операции.
        sql.append(" and mt.id = ? ");
        params.add(UUID.fromString(search));
      } else if (search.matches("\\p{Digit}{4}-\\p{Digit}{2}-\\p{Digit}{2}")) {
        // Ищем по дате в формате ISO.
        sql.append(" and mt.trn_date = ? ");
        params.add(Date.valueOf(LocalDate.parse(search, DateTimeFormatter.ISO_DATE)));
      } else if (search.matches("\\p{Digit}+\\.*\\p{Digit}*")) {
        // Ищем по сумме операции.
        // todo Переписать на balance_changes.
        sql.append(" and (mt.amount = ? or mt.to_amount = ?) ");
        IntStream.range(0, 2).forEach(i -> params.add(new BigDecimal(search)));
      } else {
        String condition = " and (mt.comment ilike ? or fa.name ilike ? or ta.name ilike ? " +
            " or exists (select null from labels2objs l2o, labels l where l2o.obj_id = mt.id and l.id = l2o.label_id and l.name ilike ?)) ";
        sql.append(condition);
        long paramNum = condition.chars().filter(ch -> ch == '?').count();
        IntStream.range(0, ((int) paramNum)).forEach(i -> params.add("%" + search + "%"));
      }
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
        trn.setBalanceChanges(getBalanceChanges(conn, trn.getId()));
        trn.setLabels(LabelsDao.getLabelNames(conn, trn.getId()));
      } catch (SQLException e) {
        throw new HmSqlException(e);
      }
    }).collect(Collectors.toList());

    return moneyTrnList;
  }

  @NotNull
  private List<MoneyTrn> getMoneyTrns(Connection conn, UUID bsId, Date trnDate) throws SQLException {
    assertNonNulls(conn, bsId, trnDate);

    return new QueryRunner().query(conn,
        baseSelect + " and mt.trn_date = ?" +
            " order by date_num, created_ts desc",
        new BeanListHandler<>(MoneyTrn.class, new BasicRowProcessor(new MoneyTrnProcessor())), bsId, trnDate)
        .stream()
        .peek(trn -> {
          try {
            trn.setBalanceChanges(getBalanceChanges(conn, trn.getId()));
            trn.setLabels(LabelsDao.getLabelNames(conn, trn.getId()));
          } catch (SQLException e) {
            e.printStackTrace();
          }
        })
        .collect(Collectors.toList());
  }

  @NotNull
  List<MoneyTrn> getMoneyTrns(Connection conn, UUID bsId, Category category) throws SQLException {
    assertNonNulls(conn, bsId, category);

    return new QueryRunner().query(conn,
        baseSelect + " and (mt.from_acc_id = ? or mt.to_acc_id = ?)",
        new BeanListHandler<>(MoneyTrn.class, new BasicRowProcessor(new MoneyTrnProcessor())), bsId, category.getId(), category.getId())
        .stream()
        .peek(trn -> {
          try {
            trn.setBalanceChanges(getBalanceChanges(conn, trn.getId()));
            trn.setLabels(LabelsDao.getLabelNames(conn, trn.getId()));
          } catch (SQLException e) {
            e.printStackTrace();
          }
        })
        .collect(Collectors.toList());
  }

  @NotNull
  private List<MoneyTrn> getTemplMoneyTrns(Connection conn, UUID bsId,
      @Nullable String search, Date beforeDate) throws SQLException {
    assertNonNulls(conn, bsId, beforeDate);

    BeanListHandler<MoneyTrn> handler = new BeanListHandler<>(MoneyTrn.class,
        new BasicRowProcessor(new MoneyTrnProcessor()));
    StringBuilder sql = new StringBuilder("" +
        "select null as id, 'recurrence' as status, null as createdTs, te.next_date as trnDate, 0 as dateNum, " +
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
        .peek(trn -> {
          try {
            trn.setId(UUID.randomUUID());
            trn.setBalanceChanges(getBalanceChanges(conn, trn.getId()));
            trn.setLabels(LabelsDao.getLabelNames(conn, trn.getTemplId()));
          } catch (SQLException e) {
            throw new HmSqlException(e);
          }
        })
        .collect(Collectors.toList());
  }


  public MoneyTrn getMoneyTrn(UUID bsId, UUID id) {
    assertNonNulls(bsId, id);
    try (Connection conn = MainDao.getConnection()) {
      return getMoneyTrn(conn, bsId, id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  MoneyTrn getMoneyTrn(Connection conn, UUID bsId, UUID id) throws SQLException {
    assertNonNulls(conn, bsId, id);
    BeanHandler<MoneyTrn> mtHandler = new BeanHandler<>(MoneyTrn.class, new BasicRowProcessor(new MoneyTrnProcessor()));
    MoneyTrn trn = new QueryRunner().query(conn, baseSelect + " and mt.id = ? ", mtHandler, bsId, id);
    if (trn != null) {
      trn.setBalanceChanges(getBalanceChanges(conn, trn.getId()));
      trn.setLabels(LabelsDao.getLabelNames(conn, id));
    }
    return trn;
  }

  public List<MoneyTrn> createMoneyTrn(UUID bsId, MoneyTrn moneyTrn) {
    assertNonNulls(bsId, moneyTrn);
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

  void createMoneyTrn(Connection conn, UUID bsId, MoneyTrn moneyTrn) throws SQLException {
    assertNonNulls(conn, bsId, moneyTrn);
    createMoneyTrn(conn, bsId, moneyTrn, new ArrayList<>(0));
  }

  private void createMoneyTrn(Connection conn, UUID bsId, MoneyTrn moneyTrn, List<MoneyTrn> result)
      throws SQLException {
    assertNonNulls(conn, bsId, moneyTrn, result);
    createMoneyTrnInternal(conn, bsId, moneyTrn);
    MoneyTrn trn1 = getMoneyTrn(conn, bsId, moneyTrn.getId());
    result.add(trn1);
    MoneyTrn trn2 = createReserveMoneyTrn(conn, bsId, moneyTrn);
    if (trn2 != null) {
      result.add(trn2);
    }
    if ((moneyTrn.getStatus() == done || moneyTrn.getStatus() == doneNew) &&
        !moneyTrn.getTrnDate().toLocalDate().isAfter(LocalDate.now())) {
      result.forEach(t -> {
        try {
          completeMoneyTrn(conn, bsId, t.getId());
        } catch (SQLException e) {
          throw new HmSqlException(e);
        }
      });
    }
  }

  private MoneyTrn createReserveMoneyTrn(Connection conn, UUID bsId, MoneyTrn moneyTrn) throws SQLException {
    assertNonNulls(conn, bsId, moneyTrn);
    BalanceSheet bs = balanceSheetRepo.findOne(bsId);
    Account svcRsv = AccountsDao.getAccount(bs.getSvcRsv().getId());

    Account fromAcc = svcRsv;
    Account account = AccountsDao.getAccount(moneyTrn.getFromAccId());
    if (Account.Type.debit == account.getType()) {
      Balance balance = balancesDao.getBalance(conn, account.getId());
      if (balance.getReserveId() != null) {
        fromAcc = reservesDao.getReserve(balance.getReserveId());
      }
    }

    Account toAcc = svcRsv;
    account = AccountsDao.getAccount(moneyTrn.getToAccId());
    if (Account.Type.debit == account.getType()) {
      Balance balance = balancesDao.getBalance(conn, account.getId());
      if (balance.getReserveId() != null) {
        toAcc = reservesDao.getReserve(balance.getReserveId());
      }
    }

    if (fromAcc != toAcc) {
      MoneyTrn rMoneyTrn = new MoneyTrn(UUID.randomUUID(), moneyTrn.getStatus(), moneyTrn.getTrnDate(),
          fromAcc.getId(), toAcc.getId(), moneyTrn.getAmount(), moneyTrn.getPeriod(), moneyTrn.getComment(), moneyTrn.getLabels(),
          moneyTrn.getDateNum(), moneyTrn.getId(), null, moneyTrn.getCreatedTs());
      createMoneyTrnInternal(conn, bsId, rMoneyTrn);
      return getMoneyTrn(conn, bsId, rMoneyTrn.getId());
    }
    return null;
  }

  private void createMoneyTrnInternal(Connection conn, UUID bsId, MoneyTrn trn) throws SQLException {
    assertNonNulls(conn, bsId, trn);
    Timestamp createdTs = new Timestamp(new java.util.Date().getTime());
    List<String> labels = trn.getLabels();

    QueryRunner run = new QueryRunner();
    run.update(conn, "" +
            "insert into money_trns(id, status, balance_sheet_id, created_ts, trn_date, date_num, " +
            "    from_acc_id, to_acc_id, parent_id, amount, to_amount, comment, period, templ_id) " +
            "  values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        trn.getId(),
        pending.name(),
        bsId,
        createdTs,
        trn.getTrnDate(),
        0,
        trn.getFromAccId(),
        trn.getToAccId(),
        trn.getParentId(),
        trn.getAmount(),
        trn.getToAmount(),
        trn.getComment(),
        trn.getPeriod().name(),
        trn.getTemplId());


    trn = getMoneyTrn(conn, bsId, trn.getId());

    if (trn.getType().equals("expense") || trn.getType().equals("transfer")) {
      createBalanceChange(conn, trn.getId(), trn.getFromAccId(), trn.getAmount().negate(), trn.getTrnDate(), 0);
    }

    if (trn.getType().equals("income") || trn.getType().equals("transfer")) {
      BigDecimal toAmount = nvl(trn.getToAmount(), trn.getAmount());
      createBalanceChange(conn, trn.getId(), trn.getToAccId(), toAmount, trn.getTrnDate(), 1);
    }

    Account fromAcc = AccountsDao.getAccount(trn.getFromAccId());
    if (fromAcc.getType() == Account.Type.income) {
      labels.add(fromAcc.getName());
    }

    Account toAcc = AccountsDao.getAccount(trn.getToAccId());
    if (toAcc.getType() == Account.Type.expense) {
      labels.add(toAcc.getName());
    }

    labels.remove("<Без категории>");

    LabelsDao.saveLabels(conn, bsId, trn.getId(), "operation", labels);
  }

  void createBalanceChange(Connection conn, UUID operId, UUID balanceId, BigDecimal value, @Nullable Date performed,
      int index) throws SQLException {
    assertNonNulls(conn, operId, balanceId, value);
    new QueryRunner().update(conn, "" +
            "insert into balance_changes (id, oper_id, balance_id, value, made, index) " +
            "  values (?, ?, ?, ?, ?, ?)",
        UUID.randomUUID(), operId, balanceId, value, performed, index);
  }

  static void updateBalanceChange(Connection conn, UUID id, UUID balanceId, BigDecimal value, @Nullable Date performed, int index) {
    assertNonNulls(id, balanceId, value);
    try {
      new QueryRunner().update(conn, "" +
          "update balance_changes set balance_id = ?, value = ?, made = ?, index = ? where id = ?",
          balanceId, value, performed, index, id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  void deleteBalanceChange(Connection conn, UUID id) {
    assertNonNulls(conn, id);
    try {
      new QueryRunner().update(conn, "delete from balance_changes where id = ?", id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private void completeMoneyTrn(Connection conn, UUID bsId, UUID moneyTrnId) throws SQLException {
    assertNonNulls(conn, bsId, moneyTrnId);
    setStatusNChangeBalanceValues(conn, bsId, moneyTrnId, done);
  }

  private void setStatusNChangeBalanceValues(Connection conn, UUID bsId, UUID moneyTrnId, MoneyTrn.Status status)
      throws SQLException {
    assertNonNulls(conn, bsId, moneyTrnId, status);
    assert status != MoneyTrn.Status.doneNew && status != MoneyTrn.Status.doneNew : status.name();

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
        Balance fromBalance = balancesDao.getBalance(conn, fromAccId);
        balancesDao.changeBalanceValue(conn, fromBalance, fromAmount.negate(), trn.getId(), status);
      }
    }

    if (toAccId != null) {
      Account toAccount = AccountsDao.getAccount(toAccId);
      if (!trn.getTrnDate().before(toAccount.getCreatedDate()) && toAccount.isBalance()) {
        Balance toBalance = balancesDao.getBalance(conn, toAccId);
        balancesDao.changeBalanceValue(conn, toBalance, toAmount, trn.getId(), status);
      }
    }

    new QueryRunner().update(conn, "update money_trns set status = ? where id = ?", status.name(), trn.getId());
  }

  public void deleteMoneyTrn(UUID bsId, UUID id) {
    assertNonNulls(bsId, id);
    try (Connection conn = MainDao.getConnection()) {
      setStatusNChangeBalanceValues(conn, bsId, id, MoneyTrn.Status.cancelled);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public void updateMoneyTrn(UUID bsId, MoneyTrn moneyTrn) {
    assertNonNulls(bsId, moneyTrn);
    try (Connection conn = MainDao.getConnection()) {
      MoneyTrn trnFromDb = getMoneyTrn(conn, bsId, moneyTrn.getId());
      if (trnFromDb != null) {
        updateMoneyTrn(conn, bsId, moneyTrn);
      } else {
        createMoneyTrn(conn, bsId, moneyTrn);
      }
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  void updateMoneyTrn(Connection conn, UUID bsId, MoneyTrn trn) throws SQLException {
    assertNonNulls(conn, bsId, trn);
    if (trn.isMonoCurrencies() && !Objects.equals(trn.getAmount(), trn.getToAmount())) {
      throw new HmException(HmException.Code.WrongAmount, trn.toString());
    }
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
      List<BalanceChange> balanceChanges = getBalanceChanges(conn, trn.getId());
      balanceChanges.stream()
          .filter(balanceChange -> balanceChange.getValue().compareTo(BigDecimal.ZERO) < 0)
          .findFirst()
          .ifPresent(balanceChange ->
              updateBalanceChange(conn, balanceChange.getId(), trn.getFromAccId(), trn.getAmount().negate(),
                  trn.getTrnDate(), balanceChange.getIndex()));
      balanceChanges.stream()
          .filter(balanceChange -> balanceChange.getValue().compareTo(BigDecimal.ZERO) > 0)
          .findFirst()
          .ifPresent(balanceChange -> {
                BigDecimal toAmount = trn.getToAmount() != null ? trn.getToAmount() : trn.getAmount();
                updateBalanceChange(conn, balanceChange.getId(), trn.getToAccId(), toAmount,
                    trn.getTrnDate(), balanceChange.getIndex());
          });
      LabelsDao.saveLabels(conn, bsId, trn.getId(), "operation", trn.getLabels());
      setStatusNChangeBalanceValues(conn, bsId, trn.getId(), origTrnStatus);
    }
  }

  static List<BalanceChange> getBalanceChanges(Connection conn, UUID operId) throws SQLException {
    assertNonNulls(conn, operId);
    return new QueryRunner().query(conn, "" +
        "select id, oper_id as operId, balance_id as balanceId, value, made, index " +
            "from balance_changes " +
            "where oper_id = ? " +
            "order by index",
        new BeanListHandler<>(BalanceChange.class), operId);
  }

  public void skipMoneyTrn(UUID bsId, MoneyTrn trn) {
    assertNonNulls(bsId, trn);
    try (Connection conn = MainDao.getConnection()) {
      if (trn.getStatus() != MoneyTrn.Status.recurrence) {
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

  public void upMoneyTrn(UUID bsId, UUID id) {
    assertNonNulls(bsId, id);
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
