package ru.serdtsev.homemoney.dao;

import com.google.common.base.Strings;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import ru.serdtsev.homemoney.dto.MoneyTrnTempl;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class MoneyTrnTemplsDao {
  private static final String baseSelect =
      "select te.id, te.status, te.sample_id as sampleId, te.last_money_trn_id lastMoneyTrnId, " +
          "    te.next_date as nextDate, te.period, " +
          "    te.from_acc_id as fromAccId, fa.name as fromAccName," +
          "    te.to_acc_id as toAccId, ta.name as toAccName," +
          "    case when fa.type = 'income' then 'income' when ta.type = 'expense' then 'expense' else 'transfer' end as type, " +
          "    te.amount, coalesce(coalesce(fb.currency_code, tb.currency_code), 'RUB') as currencyCode," +
          "    coalesce(te.to_amount, te.amount), coalesce(coalesce(tb.currency_code, fb.currency_code), 'RUB') as toCurrencyCode," +
          "    te.comment, te.labels " +
          "  from money_trn_templs te, " +
          "    accounts fa " +
          "      left join balances fb on fb.id = fa.id, " +
          "    accounts ta" +
          "      left join balances tb on tb.id = ta.id " +
          "  where te.bs_id = ? " +
          "    and fa.id = te.from_acc_id " +
          "    and ta.id = te.to_acc_id ";

  public static List<MoneyTrnTempl> getMoneyTrnTempls(UUID bsId, String search) {
    try (Connection conn = MainDao.getConnection()) {
      return getMoneyTrnTempls(conn, bsId, search);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private static List<MoneyTrnTempl> getMoneyTrnTempls(Connection conn, UUID bsId, String search) throws SQLException {
    StringBuilder sql = new StringBuilder(baseSelect + " and te.status = 'active' ");
    List<Object> params = new ArrayList<>();
    params.add(bsId);
    if (!Strings.isNullOrEmpty(search)) {
      final String condition = " and (te.comment ilike ? or te.labels ilike ? or fa.name ilike ? or ta.name ilike ?) ";
      sql.append(condition);
      String searchTempl = "%" + search + "%";
      long paramNum = condition.chars().filter(ch -> ch == '?').count();
      for (long i = 0; i < paramNum; i++) {
        params.add(searchTempl);
      }
    }
    sql.append(" order by nextDate desc ");
    return new QueryRunner().query(conn, sql.toString(),
        new BeanListHandler<>(MoneyTrnTempl.class, new BasicRowProcessor(new MoneyTrnProcessor())), params.toArray());
  }

  static MoneyTrnTempl getMoneyTrnTempl(Connection conn, UUID bsId, UUID id) throws SQLException {
    MoneyTrnTempl templ;
      String sql = baseSelect + " and te.id = ? ";
      templ = new QueryRunner().query(conn, sql,
          new BeanHandler<>(MoneyTrnTempl.class, new BasicRowProcessor(new MoneyTrnProcessor())), bsId, id);
    return templ;
  }

  public static void updateMoneyTrnTempl(UUID bsId, MoneyTrnTempl templ) {
    try (Connection conn = MainDao.getConnection()) {
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
              "  labels = ?, " +
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
          templ.getLabelsAsString(),
          templ.getPeriod().name(),
          bsId,
          templ.getId());
      if (rowCount == 0) {
        throw new IllegalArgumentException(String.format("Обновляемый шаблон %s не найден.",
            templ.getId()));
      }
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static void createMoneyTrnTempl(UUID bsId, MoneyTrnTempl templ) {
    try (Connection conn = MainDao.getConnection()) {
      new QueryRunner().update(conn,
          "insert into money_trn_templs(id, status, bs_id, sample_id, last_money_trn_id, " +
              "next_date, amount, from_acc_id, to_acc_id, comment, labels, period) " +
              "  values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          templ.getId(), templ.getStatus().name(), bsId, templ.getSampleId(), templ.getLastMoneyTrnId(),
          templ.getNextDate(), templ.getAmount(), templ.getFromAccId(), templ.getToAccId(), templ.getComment(),
          templ.getLabelsAsString(), templ.getPeriod().name());
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static void deleteMoneyTrnTempl(UUID bsId, UUID id) {
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
