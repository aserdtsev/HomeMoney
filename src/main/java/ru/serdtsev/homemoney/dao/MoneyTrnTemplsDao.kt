package ru.serdtsev.homemoney.dao

import com.google.common.base.Strings
import org.apache.commons.dbutils.BasicRowProcessor
import org.apache.commons.dbutils.DbUtils
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.BeanHandler
import org.apache.commons.dbutils.handlers.BeanListHandler
import org.apache.commons.dbutils.handlers.ScalarHandler
import ru.serdtsev.homemoney.dto.MoneyTrnTempl
import java.sql.Connection
import java.sql.SQLException
import java.util.*

object MoneyTrnTemplsDao {
  private val baseSelect =
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
          "    and ta.id = te.to_acc_id "

  fun getMoneyTrnTempls(bsId: UUID, search: String? = null): List<MoneyTrnTempl> {
    val conn = MainDao.getConnection()
    try {
      return getMoneyTrnTempls(conn, bsId, search)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  private fun getMoneyTrnTempls(conn: Connection, bsId: UUID, search: String?): List<MoneyTrnTempl> {
    try {
      val sql = StringBuilder(baseSelect + " and te.status = 'active' ")
      val params = ArrayList<Any>()
      params.add(bsId)
      if (!Strings.isNullOrEmpty(search)) {
        val condition = " and (te.comment ilike ? or te.labels ilike ? or fa.name ilike ? or ta.name ilike ?) "
        sql.append(condition)
        val searchTempl = "%$search%"
        for (i in 1..condition.count { ch -> ch == '?' }) {
          params.add(searchTempl)
        }
      }
      sql.append(" order by nextDate desc ")
      val handler = BeanListHandler(MoneyTrnTempl::class.java, BasicRowProcessor(MoneyTrnProcessor()))
      return QueryRunner().query(conn, sql.toString(), handler, *params.toArray())
    } catch (e: SQLException) {
      throw HmSqlException(e)
    }
  }

  fun getMoneyTrnTempl(conn: Connection, bsId: UUID, id: UUID): MoneyTrnTempl {
    val templ: MoneyTrnTempl
    try {
      val handler = BeanHandler(MoneyTrnTempl::class.java,
          BasicRowProcessor(MoneyTrnProcessor()))
      val run = QueryRunner()
      val sql = baseSelect + " and te.id = ? "
      templ = run.query(conn, sql, handler, bsId, id)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    }

    return templ
  }

  fun updateMoneyTrnTempl(bsId: UUID, templ: MoneyTrnTempl) {
    val conn = MainDao.getConnection()
    try {
      val run = QueryRunner()
      val rowCount = run.update(conn,
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
          templ.sampleId,
          templ.lastMoneyTrnId,
          templ.nextDate,
          templ.amount,
          templ.toAmount,
          templ.fromAccId,
          templ.toAccId,
          templ.comment,
          templ.getLabelsAsString(),
          templ.period!!.name,
          bsId,
          templ.id)
      if (rowCount == 0) {
        throw IllegalArgumentException(String.format("Обновляемый шаблон %s не найден.",
            templ.id))
      }
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  fun createMoneyTrnTempl(bsId: UUID, templ: MoneyTrnTempl) {
    val conn = MainDao.getConnection()
    try {
      QueryRunner().update(conn,
          "insert into money_trn_templs(id, status, bs_id, sample_id, last_money_trn_id, " +
              "next_date, amount, from_acc_id, to_acc_id, comment, labels, period) " +
              "  values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          templ.id, templ.status!!.name, bsId, templ.sampleId, templ.lastMoneyTrnId,
          templ.nextDate, templ.amount, templ.fromAccId, templ.toAccId, templ.comment,
          templ.getLabelsAsString(), templ.period!!.name)
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  fun deleteMoneyTrnTempl(bsId: UUID, id: UUID) {
    val conn = MainDao.getConnection()
    try {
      val sql = "update money_trn_templs set status = ? where bs_id = ? and id = ?"
      val rows = QueryRunner().update(conn, sql, MoneyTrnTempl.Status.deleted.name, bsId, id)
      if (rows == 0) {
        throw IllegalArgumentException(String.format("Удаляемый шаблон %s не найден.", id))
      }
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }

  }

  @Throws(SQLException::class)
  fun isTrnTemplExists(conn: Connection, id: UUID): Boolean {
    val trnCount = QueryRunner().query(conn, "select count(*) from money_trn_templs where from_acc_id = ? or to_acc_id = ?",
        ScalarHandler<Long>(), id, id)
    return trnCount > 0
  }

}
