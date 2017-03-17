package ru.serdtsev.homemoney.dao;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.springframework.stereotype.Component;
import ru.serdtsev.homemoney.dto.Account;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Component
public class AccountsDao {

  public static List<Account> getAccounts(UUID bsId) {
    try (Connection conn = MainDao.getConnection()) {
      return (new QueryRunner()).query(conn,
          "select a.id, a.type, a.name, a.created_date as createdDate, a.is_arc as isArc, " +
              " case when c.root_id is null then a.name " +
              " else (select name from accounts where id = c.root_id) || '#' || a.name end as sort " +
              "from accounts a left join categories c on c.id = a.id " +
              "where a.balance_sheet_id = ? " +
              "order by type, sort",
          new BeanListHandler<>(Account.class), bsId);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static Account getAccount(UUID id) {
    try (Connection conn = MainDao.getConnection()) {
      return (new QueryRunner()).query(conn,
          "select id, name, type, created_date as createdDate, is_arc as isArc " + " from accounts where id = ?",
          new BeanHandler<>(Account.class), id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static void createAccount(Connection conn, UUID bsId, Account account) throws SQLException {
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

  public static void updateAccount(Connection conn, UUID bsId, Account account) throws SQLException {
    int rows = (new QueryRunner()).update(conn,
        "update accounts set type = ?, name = ?, created_date = ?, is_arc = ? where balance_sheet_id = ? and id = ?",
        account.getType().name(), account.getName(), account.getCreatedDate(), account.getIsArc(), bsId, account.getId());
    if (rows == 0) {
      throw new IllegalArgumentException("Запись не найдена.");
    }
  }

  public static boolean isTrnExists(Connection conn, UUID id) throws SQLException{
    Long trnCount = (new QueryRunner()).query(conn, "select count(*) from money_trns where from_acc_id = ? or to_acc_id = ?",
        new ScalarHandler<Long>(), id, id);
    return trnCount > 0;
  }

}
