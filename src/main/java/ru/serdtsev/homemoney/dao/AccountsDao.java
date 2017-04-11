package ru.serdtsev.homemoney.dao;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.springframework.stereotype.Component;
import ru.serdtsev.homemoney.account.AccountDto;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class AccountsDao {

  public static void createAccount(Connection conn, UUID bsId, AccountDto account) throws SQLException {
    (new QueryRunner()).update(conn,
        "insert into accounts(id, balance_sheet_id, name, type, created_date, is_arc) values (?, ?, ?, ?, ?, ?)",
        account.getId(), bsId, account.getName(), account.getType().name(), account.getCreatedDate(), account.getIsArc());
  }

  public void deleteAccount(Connection conn, UUID bsId, UUID id) throws SQLException {
    QueryRunner run = new QueryRunner();
    int rows;
    if (isTrnExists(conn, id) || MoneyTrnTemplsDao.isTrnTemplExists(conn, id)) {
      rows = run.update(conn, "update accounts set is_arc = true where balance_sheet_id = ? and id = ?",
          bsId, id);
    } else {
      rows = run.update(conn, "delete from accounts where balance_sheet_id = ? and id = ?", bsId, id);
    }
    if (rows == 0) {
      throw new IllegalArgumentException("Запись не найдена.");
    }
  }

  public static void updateAccount(Connection conn, UUID bsId, AccountDto account) throws SQLException {
    int rows = (new QueryRunner()).update(conn,
        "update accounts set type = ?, name = ?, created_date = ?, is_arc = ? where balance_sheet_id = ? and id = ?",
        account.getType().name(), account.getName(), account.getCreatedDate(), account.getIsArc(), bsId, account.getId());
    if (rows == 0) {
      throw new IllegalArgumentException("Запись не найдена.");
    }
  }

  public static boolean isTrnExists(UUID id) {
    try (Connection conn = MainDao.getConnection()) {
      return isTrnExists(conn, id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static boolean isTrnExists(Connection conn, UUID id) throws SQLException{
    Long trnCount = (new QueryRunner()).query(conn, "select count(*) from money_trns where from_acc_id = ? or to_acc_id = ?",
        new ScalarHandler<Long>(), id, id);
    return trnCount > 0;
  }

}
