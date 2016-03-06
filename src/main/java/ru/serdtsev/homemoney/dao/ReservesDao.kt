package ru.serdtsev.homemoney.dao

import org.apache.commons.dbutils.DbUtils
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.BeanHandler
import org.apache.commons.dbutils.handlers.BeanListHandler
import ru.serdtsev.homemoney.dto.Account
import ru.serdtsev.homemoney.dto.Balance
import ru.serdtsev.homemoney.dto.Reserve
import java.sql.Connection
import java.sql.SQLException
import java.util.*

object ReservesDao {
  fun getReserves(bsId: UUID): List<Reserve> {
    val conn = MainDao.getConnection()
    try {
      return QueryRunner().query(conn,
          "select a.id, a.name, a.type, b.value, a.created_date as createdDate, a.is_arc arc, r.target " +
              "from accounts a, balances b, reserves r " +
              "where a.balance_sheet_id = ? and b.id = a.id and r.id = a.id " +
              "order by a.created_date desc",
          BeanListHandler(Reserve::class.java), bsId)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  fun getReserve(id: UUID): Reserve {
    val conn = MainDao.getConnection()
    try {
      return getReserve(conn, id)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  @Throws(SQLException::class)
  private fun getReserve(conn: Connection, id: UUID): Reserve {
    val reserve: Reserve
    val h = BeanHandler(Reserve::class.java)
    val run = QueryRunner()
    reserve = run.query(conn,
        "select a.id, a.name, a.type, b.value, a.created_date as createdDate, a.is_arc as arc, r.target " +
            "from accounts a, balances b, reserves r " +
            "where a.id = ? and b.id = r.id and r.id = a.id",
        h, id)
    return reserve
  }

  fun createReserve(bsId: UUID, reserve: Reserve) {
    val conn = MainDao.getConnection()
    try {
      createReserve(conn, bsId, reserve)
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }

  }

  @Throws(SQLException::class)
  fun createReserve(conn: Connection, bsId: UUID, reserve: Reserve) {
    val balance = Balance(reserve.id!!, Account.Type.reserve, reserve.name!!, reserve.value!!)
    BalancesDao.createBalance(conn, bsId, balance)
    QueryRunner().update(conn,
        "insert into reserves(id, target) values (?, ?)",
        reserve.id, reserve.target)
  }

  fun deleteReserve(bsId: UUID, id: UUID) {
    val conn = MainDao.getConnection()
    try {
      if (!AccountsDao.isTrnExists(conn, id) && !MoneyTrnTemplsDao.isTrnTemplExists(conn, id)) {
        val rows = QueryRunner().update(conn, "delete from reserves where id = ?", id)
        if (rows == 0) {
          throw IllegalArgumentException("Неверные параметры запроса.")
        }
      }
      BalancesDao.deleteBalance(conn, bsId, id)
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }

  }

  fun updateReserve(bsId: UUID, reserve: Reserve) {
    val conn = MainDao.getConnection()
    try {
      BalancesDao.updateBalance(conn, bsId, reserve)
      QueryRunner().update(conn,
          "update reserves set target = ? where id = ?",
          reserve.target, reserve.id)
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

}
