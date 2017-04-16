package ru.serdtsev.homemoney.account;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.springframework.stereotype.Component;
import ru.serdtsev.homemoney.dao.HmSqlException;
import ru.serdtsev.homemoney.dao.MainDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class AccountsDao {
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
