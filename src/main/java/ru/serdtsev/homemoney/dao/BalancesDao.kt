package ru.serdtsev.homemoney.dao

import org.apache.commons.dbutils.DbUtils
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.BeanHandler
import org.apache.commons.dbutils.handlers.BeanListHandler
import org.slf4j.LoggerFactory
import ru.serdtsev.homemoney.dto.Balance
import ru.serdtsev.homemoney.dto.MoneyTrn
import java.awt.IllegalComponentStateException
import java.math.BigDecimal
import java.sql.Connection
import java.sql.SQLException
import java.time.LocalDate
import java.util.*

object BalancesDao {
  private val log = LoggerFactory.getLogger(javaClass)
  fun getBalances(bsId: UUID): List<Balance> {
    val conn = MainDao.getConnection()
    return try {
      getBalances(conn, bsId)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  @Throws(SQLException::class)
  private fun getBalances(conn: Connection, bsId: UUID): MutableList<Balance> =
    QueryRunner().query(conn,
        "select a.id, a.type, a.name, b.currency_code as currencyCode, b.value, a.created_date as createdDate, " +
            " a.is_arc as arc, b.reserve_id as reserveId," +
            " coalesce(b.credit_limit, 0) as creditLimit, coalesce(b.min_value, 0) as minValue, b.num" +
            " from accounts a, balances b " +
            " where a.balance_sheet_id = ? and a.type in ('debit', 'credit', 'asset') and b.id = a.id " +
            " order by num, a.created_date desc",
        BeanListHandler(Balance::class.java), bsId)

  @Throws(SQLException::class)
  fun getBalance(conn: Connection, id: UUID): Balance =
    QueryRunner().query(conn,
        "select a.id, a.type, a.name, b.currency_code as currencyCode, b.value, a.created_date as createdDate, " +
            " a.is_arc as arc, b.reserve_id as reserveId," +
            " coalesce(b.credit_limit, 0) as creditLimit, coalesce(b.min_value, 0) as minValue" +
            " from accounts a, balances b" +
            " where a.id = ? and b.id = a.id",
        BeanHandler(Balance::class.java), id)

  fun createBalance(bsId: UUID, balance: Balance) {
    val conn = MainDao.getConnection()
    try {
      createBalance(conn, bsId, balance)
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  @Throws(SQLException::class)
  fun createBalance(conn: Connection, bsId: UUID, balance: Balance) {
    AccountsDao.createAccount(conn, bsId, balance)
    QueryRunner().update(conn,
        "insert into balances(id, currency_code, value, reserve_id, credit_limit, min_value) values (?, ?, ?, ?, ?, ?)",
        balance.id, balance.currencyCode, balance.value, balance.reserveId, balance.creditLimit, balance.minValue)
  }

  fun deleteBalance(bsId: UUID, id: UUID) {
    val conn = MainDao.getConnection()
    try {
      deleteBalance(conn, bsId, id)
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  @Throws(SQLException::class)
  internal fun deleteBalance(conn: Connection, bsId: UUID, id: UUID) {
    if (!AccountsDao.isTrnExists(conn, id) && !MoneyTrnTemplsDao.isTrnTemplExists(conn, id)) {
      val rows = QueryRunner().update(conn, "delete from balances where id = ?", id)
      if (rows == 0) {
        throw IllegalArgumentException("Неверные параметры запроса.")
      }
    }
    AccountsDao.deleteAccount(conn, bsId, id)
  }

  fun updateBalance(bsId: UUID, balance: Balance) {
    val conn = MainDao.getConnection()
    try {
      updateBalance(conn, bsId, balance)
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  @Throws(SQLException::class)
  internal fun updateBalance(conn: Connection, bsId: UUID, balance: Balance) {
    AccountsDao.updateAccount(conn, bsId, balance)
    val currBalance = getBalance(conn, balance.id!!)
    if (balance.value!!.compareTo(currBalance.value) != 0) {
      val bs = MainDao.getBalanceSheet(bsId)
      val more = balance.value!! > currBalance.value
      val fromAccId = if (more) bs.uncatIncomeId else balance.id
      val toAccId = if (more) balance.id else bs.uncatCostsId
      val amount = balance.value!!.subtract(currBalance.value).abs()
      val moneyTrn = MoneyTrn(
          UUID.randomUUID(), MoneyTrn.Status.done, java.sql.Date.valueOf(LocalDate.now()), fromAccId!!, toAccId!!, amount,
          MoneyTrn.Period.single, "корректировка остатка")
      MoneyTrnsDao.createMoneyTrn(conn, bsId, moneyTrn)
    }

    val run = QueryRunner()
    run.update(conn,
        "update balances set value = ?, reserve_id = ?, credit_limit = ?, min_value = ?" + " where id = ?",
        balance.value, balance.reserveId, balance.creditLimit,
        balance.minValue, balance.id)
  }

  fun upBalance(bsId: UUID, balance: Balance) {
    val conn = MainDao.getConnection()
    try {
      val list = getBalances(conn, bsId)
      val index = list.indexOf(balance)
      if (index > 0) {
        val prev = list[index - 1]
        list[index - 1] = balance
        list[index] = prev
        val run = QueryRunner()
        var i = 0
        for (p in list) {
          run.update(conn, "update balances set num = ? where id = ?", i, p.id)
          i++
        }
      }
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  @Throws(SQLException::class)
  fun changeBalanceValue(conn: Connection, balance: Balance, amount: BigDecimal, trnId: UUID, status: MoneyTrn.Status) {
    val run = QueryRunner()
    val rows = run.update(conn, "update balances set value = value + ? where id = ?", amount, balance.id)
    if (rows == 0) {
      throw IllegalComponentStateException(String.format("Баланс счета {%s} не был изменен в БД.", balance.id))
    }
    log.info("Balance value changed; " +
        "accId:${balance.id}, " +
        "trnId:$trnId, " +
        "status:${status.name}, " +
        "before:${balance.value}, " +
        "after:${getBalance(conn, balance.id!!).value}.")
  }
}
