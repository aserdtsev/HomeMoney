package ru.serdtsev.homemoney.dao

import org.apache.commons.dbutils.DbUtils
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.BeanHandler
import org.apache.commons.dbutils.handlers.BeanListHandler
import org.apache.commons.dbutils.handlers.ScalarHandler
import ru.serdtsev.homemoney.dto.Account
import java.sql.Connection
import java.sql.SQLException
import java.util.*

object AccountsDao {
  fun getAccounts(balanceSheetId: UUID): List<Account> {
    val conn = MainDao.getConnection()
    try {
      return QueryRunner().query(conn,
          "select a.id, a.type, a.name, a.created_date as createdDate, a.is_arc as isArc, " +
              " case when c.root_id is null then a.name " +
              " else (select name from accounts where id = c.root_id) || '#' || a.name end as sort " +
              "from accounts a left join categories c on c.id = a.id " +
              "where a.balance_sheet_id = ? " +
              "order by type, sort",
          BeanListHandler(Account::class.java), balanceSheetId)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  fun getAccount(id: UUID): Account {
    val conn = MainDao.getConnection()
    try {
      return QueryRunner().query(conn,
          "select id, name, type, created_date as createdDate, is_arc as isArc " + " from accounts where id = ?",
          BeanHandler(Account::class.java), id)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  @Throws(SQLException::class)
  fun createAccount(conn: Connection, bsId: UUID, account: Account) {
    val run = QueryRunner()
    run.update(conn,
        "insert into accounts(id, balance_sheet_id, name, type, created_date, is_arc) values (?, ?, ?, ?, ?, ?)",
        account.id, bsId, account.name, account.type!!.name, account.createdDate, account.getIsArc())
  }

  @Throws(SQLException::class)
  fun deleteAccount(conn: Connection, bsId: UUID, id: UUID) {
    val run = QueryRunner()
    val rows: Int
    if (isTrnExists(conn, id) || MoneyTrnTemplsDao.isTrnTemplExists(conn, id)) {
      rows = run.update(conn, "update accounts set is_arc = true where balance_sheet_id = ? and id = ?",
          bsId, id)
    } else {
      rows = run.update(conn, "delete from accounts where balance_sheet_id = ? and id = ?", bsId, id)
    }
    if (rows == 0) {
      throw IllegalArgumentException("Запись не найдена.")
    }
  }

  @Throws(SQLException::class)
  fun updateAccount(conn: Connection, bsId: UUID, account: Account) {
    val rows = QueryRunner().update(conn,
        "update accounts set type = ?, name = ?, created_date = ?, is_arc = ? where balance_sheet_id = ? and id = ?",
        account.type!!.name, account.name, account.createdDate, account.getIsArc(), bsId, account.id)
    if (rows == 0) {
      throw IllegalArgumentException("Запись не найдена.")
    }
  }

  @Throws(SQLException::class)
  fun isTrnExists(conn: Connection, id: UUID): Boolean {
    val trnCount = QueryRunner().query(conn, "select count(*) from money_trns where from_acc_id = ? or to_acc_id = ?",
        ScalarHandler<Long>(), id, id)
    return trnCount > 0
  }

}
