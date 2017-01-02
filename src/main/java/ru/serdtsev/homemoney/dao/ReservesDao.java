package ru.serdtsev.homemoney.dao;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
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
  public static List<Reserve> getReserves(UUID bsId) {
    try (Connection conn = MainDao.getConnection()) {
      return (new QueryRunner()).query(conn,
          "select a.id, a.name, a.type, b.value, a.created_date as createdDate, a.is_arc as isArc, b.currency_code as currencyCode, r.target " +
              "from accounts a, balances b, reserves r " +
              "where a.balance_sheet_id = ? and b.id = a.id and r.id = a.id " +
              "order by a.created_date desc",
          new BeanListHandler<>(Reserve.class), bsId);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static Reserve getReserve(UUID id) {
    try (Connection conn = MainDao.getConnection()) {
      return getReserve(conn, id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private static Reserve getReserve(Connection conn, UUID id) throws SQLException {
    return (new QueryRunner()).query(conn,
        "select a.id, a.name, a.type, b.value, a.created_date as createdDate, a.is_arc as isArc, r.target " +
            "from accounts a, balances b, reserves r " +
            "where a.id = ? and b.id = r.id and r.id = a.id",
        new BeanHandler<>(Reserve.class), id);
  }

  public static void createReserve(UUID bsId, Reserve reserve) {
    try (Connection conn = MainDao.getConnection()) {
      createReserve(conn, bsId, reserve);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }

  }

  private static void createReserve(Connection conn, UUID bsId, Reserve reserve) throws SQLException {
    Balance balance = new Balance(reserve.getId(), Account.Type.reserve, reserve.getName(),
        reserve.getCurrencyCode(), reserve.getValue());
    BalancesDao.createBalance(conn, bsId, balance);
    (new QueryRunner()).update(conn,
        "insert into reserves(id, target) values (?, ?)",
        reserve.getId(), reserve.getTarget());
  }

  public static void deleteReserve(UUID bsId, UUID id) {
    try (Connection conn = MainDao.getConnection()) {
      if (!AccountsDao.isTrnExists(conn, id) && !MoneyTrnTemplsDao.isTrnTemplExists(conn, id)) {
        int rows = (new QueryRunner()).update(conn, "delete from reserves where id = ?", id);
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
      (new QueryRunner()).update(conn,
          "update reserves set target = ? where id = ?",
          reserve.getTarget(), reserve.getId());
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }
}
