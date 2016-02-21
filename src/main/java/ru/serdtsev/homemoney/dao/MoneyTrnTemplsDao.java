package ru.serdtsev.homemoney.dao;

import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import ru.serdtsev.homemoney.dto.MoneyTrnTempl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MoneyTrnTemplsDao {
  private static final String baseSelect =
      "select te.id, te.status, te.sample_id as sampleId, te.last_money_trn_id lastMoneyTrnId, " +
          "    te.next_date as nextDate, te.period, " +
          "    te.from_acc_id as fromAccId, fa.name as fromAccName," +
          "    te.to_acc_id as toAccId, ta.name as toAccName," +
          "    case when fa.type = 'income' then 'income' when ta.type = 'expense' then 'expense' else 'transfer' end as type, " +
          "    te.amount, te.comment, te.labels " +
          "  from money_trn_templs te, accounts fa, accounts ta " +
          "  where te.bs_id = ? " +
          "    and fa.id = te.from_acc_id and ta.id = te.to_acc_id ";

  public static List<MoneyTrnTempl> getMoneyTrnTempls(UUID bsId, Optional<String> search) {
    try (Connection conn = MainDao.getConnection()) {
      return getMoneyTrnTempls(conn, bsId, search);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private static List<MoneyTrnTempl> getMoneyTrnTempls(Connection conn, UUID bsId, Optional<String> search) {
    List<MoneyTrnTempl> list;
    try {
      ResultSetHandler<List<MoneyTrnTempl>> handler = new BeanListHandler<>(MoneyTrnTempl.class,
          new BasicRowProcessor((new MoneyTrnProcessor())));
      QueryRunner run = new QueryRunner();
      final StringBuilder sql = new StringBuilder(baseSelect + " and te.status = 'active' ");
      final List<Object> params = new ArrayList<>();
      params.add(bsId);
      search.ifPresent(s -> {
        sql.append(" and (te.comment ilike ? or te.labels ilike ? or fa.name ilike ? or ta.name ilike ?) ");
        final String searchTempl = "%" + s + "%";
        for (int i = 0; i < 4; i++) {
          params.add(searchTempl);
        }
      });
      sql.append(" order by nextDate desc ");
      list = run.query(conn, sql.toString(), handler, params.toArray());
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return list;
  }

  public static MoneyTrnTempl getMoneyTrnTempl(Connection conn, UUID bsId, UUID id) {
    MoneyTrnTempl templ;
    try {
      ResultSetHandler<MoneyTrnTempl> handler = new BeanHandler<>(MoneyTrnTempl.class,
          new BasicRowProcessor((new MoneyTrnProcessor())));
      QueryRunner run = new QueryRunner();
      String sql = baseSelect + " and te.id = ? ";
      templ = run.query(conn, sql, handler, bsId, id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return templ;
  }

  public static void updateMoneyTrnTempl(UUID bsId, MoneyTrnTempl templ) {
    try (Connection conn = MainDao.getConnection()) {
      QueryRunner run = new QueryRunner();
      int rowCount = run.update(conn,
          "update money_trn_templs set " +
              "  sample_id = ?, " +
              "  last_money_trn_id = ?, " +
              "  next_date = ?, " +
              "  amount = ?, " +
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
      QueryRunner run = new QueryRunner();
      run.update(conn,
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
      QueryRunner run = new QueryRunner();
      String sql = "update money_trn_templs set status = ? where bs_id = ? and id = ?";
      int rows = run.update(conn, sql, MoneyTrnTempl.Status.deleted.name(), bsId, id);
      if (rows == 0) {
        throw new IllegalArgumentException(String.format("Удаляемый шаблон %s не найден.", id));
      }
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static Boolean isTrnTemplExists(Connection conn, UUID id) throws SQLException {
    QueryRunner run = new QueryRunner();
    Long trnCount = run.query(conn, "select count(*) from money_trn_templs where from_acc_id = ? or to_acc_id = ?",
        new ScalarHandler<>(), id, id);
    return trnCount > 0;
  }

}
