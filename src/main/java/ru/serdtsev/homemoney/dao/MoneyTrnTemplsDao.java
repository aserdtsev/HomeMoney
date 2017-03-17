package ru.serdtsev.homemoney.dao;

import com.google.common.base.Strings;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.serdtsev.homemoney.dto.Account;
import ru.serdtsev.homemoney.dto.BalanceChange;
import ru.serdtsev.homemoney.dto.MoneyTrnTempl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.serdtsev.homemoney.utils.Utils.nvl;

@Component
public class MoneyTrnTemplsDao {
  private static final String baseSelect =
      "select te.id, te.status, te.sample_id as sampleId, te.last_money_trn_id lastMoneyTrnId, " +
          "    te.next_date as nextDate, te.period, " +
          "    te.from_acc_id as fromAccId, fa.name as fromAccName," +
          "    te.to_acc_id as toAccId, ta.name as toAccName," +
          "    case when fa.type = 'income' then 'income' when ta.type = 'expense' then 'expense' else 'transfer' end as type, " +
          "    te.amount, coalesce(coalesce(fb.currency_code, tb.currency_code), 'RUB') as currencyCode," +
          "    coalesce(te.to_amount, te.amount), coalesce(coalesce(tb.currency_code, fb.currency_code), 'RUB') as toCurrencyCode," +
          "    te.comment " +
          "  from money_trn_templs te, " +
          "    accounts fa " +
          "      left join balances fb on fb.id = fa.id, " +
          "    accounts ta" +
          "      left join balances tb on tb.id = ta.id " +
          "  where te.bs_id = ? " +
          "    and fa.id = te.from_acc_id " +
          "    and ta.id = te.to_acc_id ";

  private MoneyTrnsDao moneyTrnsDao;

  @Autowired
  public MoneyTrnTemplsDao(MoneyTrnsDao moneyTrnsDao) {
    this.moneyTrnsDao = moneyTrnsDao;
  }

  public List<MoneyTrnTempl> getMoneyTrnTempls(UUID bsId, String search) {
    try (Connection conn = MainDao.getConnection()) {
      return getMoneyTrnTempls(conn, bsId, search);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  List<MoneyTrnTempl> getMoneyTrnTempls(Connection conn, UUID bsId, String search) throws SQLException {
    StringBuilder sql = new StringBuilder(baseSelect + " and te.status = 'active' ");
    List<Object> params = new ArrayList<>();
    params.add(bsId);
    if (!Strings.isNullOrEmpty(search)) {
      final String condition = " and (te.comment ilike ? or fa.name ilike ? or ta.name ilike ? " +
          " or exists (select null from labels2objs l2o, labels l where l2o.obj_id = te.id and l.id = l2o.label_id and l.name ilike ?)) ";
      sql.append(condition);
      long paramNum = condition.chars().filter(ch -> ch == '?').count();
      IntStream.range(0, ((int) paramNum)).forEach(i -> params.add("%" + search + "%"));
    }
    sql.append(" order by nextDate desc ");
    return new QueryRunner().query(conn, sql.toString(),
        new BeanListHandler<>(MoneyTrnTempl.class, new BasicRowProcessor(new MoneyTrnProcessor())), params.toArray()).stream()
        .peek(templ -> {
          try {
            templ.setBalanceChanges(moneyTrnsDao.getBalanceChanges(conn, templ.getId()));
            templ.setLabels(LabelsDao.getLabelNames(conn, templ.getId()));
          } catch (SQLException e) {
            throw new HmSqlException(e);
          }
        })
        .collect(Collectors.toList());
  }

  static MoneyTrnTempl getMoneyTrnTempl(Connection conn, UUID bsId, UUID id) throws SQLException {
    MoneyTrnTempl templ;
      String sql = baseSelect + " and te.id = ? ";
      templ = new QueryRunner().query(conn, sql,
          new BeanHandler<>(MoneyTrnTempl.class, new BasicRowProcessor(new MoneyTrnProcessor())), bsId, id);
      templ.setBalanceChanges(MoneyTrnsDao.getBalanceChanges(conn, templ.getId()));
      templ.setLabels(LabelsDao.getLabelNames(conn, id));
    return templ;
  }

  public static void updateMoneyTrnTempl(UUID bsId, MoneyTrnTempl templ) {
    try (Connection conn = MainDao.getConnection()) {
      updateMoneyTrnTempl(conn, bsId, templ);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  static void updateMoneyTrnTempl(Connection conn, UUID bsId, MoneyTrnTempl templ) throws SQLException {
    int rowCount = new QueryRunner().update(conn,
        "update money_trn_templs set " +
            "  sample_id = ?, " +
            "  last_money_trn_id = ?, " +
            "  next_date = ?, " +
            "  amount = ?, " +
            "  to_amount = ?, " +
            "  from_acc_id = ?, " +
            "  to_acc_id = ?, " +
            "  comment = ?, " +
            "  period = ? " +
            " where bs_id = ? and id = ?",
        templ.getSampleId(),
        templ.getLastMoneyTrnId(),
        templ.getNextDate(),
        templ.getAmount(),
        templ.getToAmount(),
        templ.getFromAccId(),
        templ.getToAccId(),
        templ.getComment(),
        templ.getPeriod().name(),
        bsId,
        templ.getId());
    if (rowCount == 0) {
      throw new IllegalArgumentException(String.format("Обновляемый шаблон %s не найден.",
          templ.getId()));
    }

    List<BalanceChange> balanceChanges = MoneyTrnsDao.getBalanceChanges(conn, templ.getId());
    balanceChanges.stream()
        .filter(balanceChange -> balanceChange.getValue().compareTo(BigDecimal.ZERO) < 0)
        .findFirst()
        .ifPresent(balanceChange ->
            MoneyTrnsDao.updateBalanceChange(conn, balanceChange.getId(), templ.getFromAccId(), templ.getAmount().negate(),
                null, balanceChange.getIndex()));
    balanceChanges.stream()
        .filter(balanceChange -> balanceChange.getValue().compareTo(BigDecimal.ZERO) > 0)
        .findFirst()
        .ifPresent(balanceChange -> {
          BigDecimal toAmount = templ.getToAmount() != null ? templ.getToAmount() : templ.getAmount();
          MoneyTrnsDao.updateBalanceChange(conn, balanceChange.getId(), templ.getToAccId(), toAmount,
              null, balanceChange.getIndex());
        });

    LabelsDao.saveLabels(conn, bsId, templ.getId(), "template", templ.getLabels());
  }

  public void createMoneyTrnTempl(UUID bsId, MoneyTrnTempl templ) {
    try (Connection conn = MainDao.getConnection()) {
      List<String> labels = templ.getLabels();

      new QueryRunner().update(conn,
          "insert into money_trn_templs(id, status, bs_id, sample_id, last_money_trn_id, " +
              "next_date, amount, from_acc_id, to_acc_id, comment, period) " +
              "  values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          templ.getId(), templ.getStatus().name(), bsId, templ.getSampleId(), templ.getLastMoneyTrnId(),
          templ.getNextDate(), templ.getAmount(), templ.getFromAccId(), templ.getToAccId(), templ.getComment(),
          templ.getPeriod().name());

      templ = getMoneyTrnTempl(conn, bsId, templ.getId());

      if (templ.getType().equals("expense") || templ.getType().equals("transfer")) {
        moneyTrnsDao.createBalanceChange(conn, templ.getId(), templ.getFromAccId(), templ.getAmount().negate(), null, 0);
      }

      if (templ.getType().equals("income") || templ.getType().equals("transfer")) {
        BigDecimal toAmount = nvl(templ.getToAmount(), templ.getAmount());
        moneyTrnsDao.createBalanceChange(conn, templ.getId(), templ.getToAccId(), toAmount, null, 1);
      }

      Account fromAcc = AccountsDao.getAccount(templ.getFromAccId());
      if (fromAcc.getType() == Account.Type.income) {
        labels.add(fromAcc.getName());
      }

      Account toAcc = AccountsDao.getAccount(templ.getToAccId());
      if (toAcc.getType() == Account.Type.expense) {
        labels.add(toAcc.getName());
      }

      labels.remove("<Без категории>");

      LabelsDao.saveLabels(conn, bsId, templ.getId(), "template", templ.getLabels());
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public void deleteMoneyTrnTempl(UUID bsId, UUID id) {
    try (Connection conn = MainDao.getConnection()) {
      String sql = "update money_trn_templs set status = ? where bs_id = ? and id = ?";
      int rows = new QueryRunner().update(conn, sql, MoneyTrnTempl.Status.deleted.name(), bsId, id);
      if (rows == 0) {
        throw new IllegalArgumentException(String.format("Удаляемый шаблон %s не найден.", id));
      }
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  static Boolean isTrnTemplExists(Connection conn, UUID id) throws SQLException {
    long trnCount = new QueryRunner().query(conn,
        "select count(*) from money_trn_templs where from_acc_id = ? or to_acc_id = ?",
        new ScalarHandler<Long>(), id, id);
    return trnCount > 0;
  }
}
