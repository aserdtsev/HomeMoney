package ru.serdtsev.homemoney.dao;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import ru.serdtsev.homemoney.dto.Account;
import ru.serdtsev.homemoney.dto.Balance;
import ru.serdtsev.homemoney.dto.Reserve;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class ReservesDao {
  public static List<Reserve> getReserves(UUID balanceSheetId) {
    List<Reserve> list;
    try (Connection conn = MainDao.getConnection()) {
      ResultSetHandler<List<Reserve>> h = new BeanListHandler<>(Reserve.class);
      QueryRunner run = new QueryRunner();
      list = run.query(conn,
          "select a.id, a.name, a.type, b.value, a.created_date as createdDate, a.is_arc arc, r.target " +
              "from accounts a, balances b, reserves r " +
              "where a.balance_sheet_id = ? and b.id = a.id and r.id = a.id " +
              "order by a.created_date desc",
          h, balanceSheetId);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return list;
  }

  public static Reserve getReserve(UUID id) {
    Reserve reserve;
    try (Connection conn = MainDao.getConnection()) {
      reserve = getReserve(conn, id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return reserve;
  }

  private static Reserve getReserve(Connection conn, UUID id) throws SQLException {
    Reserve reserve;
    ResultSetHandler<Reserve> h = new BeanHandler<>(Reserve.class);
    QueryRunner run = new QueryRunner();
    reserve = run.query(conn,
        "select a.id, a.name, a.type, b.value, a.created_date as createdDate, a.is_arc as arc, r.target " +
            "from accounts a, balances b, reserves r " +
            "where a.id = ? and b.id = r.id and r.id = a.id",
        h, id);
    return reserve;
  }

  public static void createReserve(UUID balanceSheetId, Reserve reserve) {
    try (Connection conn = MainDao.getConnection()) {
      createReserve(conn, balanceSheetId, reserve);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static void createReserve(Connection conn, UUID bsId, Reserve reserve) throws SQLException {
    Balance balance = new Balance(reserve.getId(), Account.Type.reserve, reserve.getName(), reserve.getValue(),
        null, null, null);
    BalancesDao.createBalance(conn, bsId, balance);
    QueryRunner run = new QueryRunner();
    run.update(conn,
        "insert into reserves(id, target) values (?, ?)",
        reserve.getId(), reserve.getTarget());
  }

  public static void deleteReserve(UUID bsId, UUID id) {
    try (Connection conn = MainDao.getConnection()) {
      if (!AccountsDao.isTrnExists(conn, id) && !MoneyTrnTemplsDao.isTrnTemplExists(conn, id)) {
        QueryRunner run = new QueryRunner();
        int rows = run.update(conn, "delete from reserves where id = ?", id);
        if (rows == 0) {
          throw new IllegalArgumentException("Неверные параметры запроса.");
        }
      }
      BalancesDao.deleteBalance(conn, bsId, id);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static void updateReserve(UUID bsId, Reserve reserve) {
    try (Connection conn = MainDao.getConnection()) {
      BalancesDao.updateBalance(conn, bsId, reserve);
      QueryRunner run = new QueryRunner();
      run.update(conn,
          "update reserves set target = ? where id = ?",
          reserve.getTarget(), reserve.getId());
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

}
