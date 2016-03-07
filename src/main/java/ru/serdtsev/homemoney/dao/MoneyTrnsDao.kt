package ru.serdtsev.homemoney.dao

import com.google.common.base.Strings
import org.apache.commons.dbutils.BasicRowProcessor
import org.apache.commons.dbutils.DbUtils
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.BeanHandler
import org.apache.commons.dbutils.handlers.BeanListHandler
import ru.serdtsev.homemoney.HmException
import ru.serdtsev.homemoney.dto.Account
import ru.serdtsev.homemoney.dto.Balance
import ru.serdtsev.homemoney.dto.MoneyTrn
import ru.serdtsev.homemoney.dto.MoneyTrn.Status
import ru.serdtsev.homemoney.dto.MoneyTrn.Status.done
import ru.serdtsev.homemoney.dto.MoneyTrn.Status.pending
import ru.serdtsev.homemoney.dto.MoneyTrnTempl
import java.sql.Connection
import java.sql.Date
import java.sql.SQLException
import java.time.LocalDate
import java.util.*
import javax.validation.constraints.NotNull

object MoneyTrnsDao {
  private val baseSelect =
      "select mt.id, mt.status, mt.created_ts as createdTs, mt.trn_date as trnDate, mt.date_num as dateNum," +
          " mt.from_acc_id as fromAccId, fa.name as fromAccName, " +
          " mt.to_acc_id as toAccId, ta.name as toAccName, " +
          " case when fa.type = 'income' then 'income' when ta.type = 'expense' then 'expense' else 'transfer' end as type, " +
          " mt.parent_id as parentId, mt.amount, " +
          " mt.comment, mt.labels, mt.period, mt.templ_id as templId " +
          " from money_trns mt, accounts fa, accounts ta " +
          " where mt.balance_sheet_id = ? " +
          " and fa.id = mt.from_acc_id " +
          " and ta.id = mt.to_acc_id "

  fun getDoneMoneyTrns(bsId: UUID, search: String?, limit: Int, offset: Int): List<MoneyTrn> {
    val trns: List<MoneyTrn>
    val conn = MainDao.getConnection()
    try {
      trns = getMoneyTrns(conn, bsId, Optional.of(MoneyTrn.Status.done), Optional.ofNullable<String>(Strings.emptyToNull(search)),
          Optional.empty<Date>(), Optional.of(limit), Optional.of(offset))
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }

    return trns
  }

  fun getPendingMoneyTrns(bsId: UUID, search: String?, beforeDate: Date): List<MoneyTrn> {
    val trns: MutableList<MoneyTrn>
    val conn = MainDao.getConnection()
    try {
      trns = getMoneyTrns(conn, bsId, Optional.of(MoneyTrn.Status.pending), Optional.ofNullable(search), Optional.of(beforeDate),
          Optional.empty<Int>(), Optional.empty<Int>())
      trns.addAll(getTemplMoneyTrns(conn, bsId, Optional.ofNullable<String>(Strings.emptyToNull(search)), beforeDate))
      Collections.sort(trns) { t1, t2 -> t2.trnDate!!.compareTo(t1.trnDate) }
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }

    return trns
  }

  fun getMoneyTrns(conn: Connection, bsId: UUID, status: Optional<Status>,
                   search: Optional<String>, beforeDate: Optional<Date>, limit: Optional<Int>, offset: Optional<Int>): MutableList<MoneyTrn> {
    val moneyTrnList: List<MoneyTrn>
    try {
      val mtHandler = BeanListHandler(MoneyTrn::class.java,
          BasicRowProcessor(MoneyTrnProcessor()))
      val run = QueryRunner()

      val sql = StringBuilder(baseSelect)
      val params = ArrayList<Any>()
      params.add(bsId)

      status.ifPresent { s ->
        sql.append(" and status = ? ")
        params.add(s.name)
      }

      beforeDate.ifPresent { d ->
        sql.append(" and mt.trn_date < ? ")
        params.add(d)
      }

      search.ifPresent { s ->
        sql.append(" and (mt.comment ilike ? or mt.labels ilike ? or fa.name ilike ? or ta.name ilike ?)")
        val searchTempl = "%$s%"
        for (i in 0..3) {
          params.add(searchTempl)
        }
      }

      sql.append(" order by trn_date desc, date_num, created_ts desc ")

      limit.ifPresent { l ->
        sql.append(" limit ? ")
        params.add(l)
      }

      offset.ifPresent { o ->
        sql.append(" offset ? ")
        params.add(o)
      }

      moneyTrnList = run.query(conn, sql.toString(), mtHandler, *params.toArray())
    } catch (e: SQLException) {
      throw HmSqlException(e)
    }

    return moneyTrnList
  }

  fun getMoneyTrns(conn: Connection, bsId: UUID, trnDate: Date): MutableList<MoneyTrn> {
    val moneyTrnList: List<MoneyTrn>
    try {
      val mtHandler = BeanListHandler(MoneyTrn::class.java,
          BasicRowProcessor(MoneyTrnProcessor()))
      val run = QueryRunner()

      moneyTrnList = run.query(conn,
          baseSelect + " and mt.trn_date = ?" +
              " order by date_num, created_ts desc",
          mtHandler, bsId, trnDate)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    }

    return moneyTrnList
  }

  private fun getTemplMoneyTrns(conn: Connection, bsId: UUID, search: Optional<String>, beforeDate: Date): List<MoneyTrn> {
    try {
      val handler = BeanListHandler(MoneyTrn::class.java,
          BasicRowProcessor(MoneyTrnProcessor()))
      val sql = StringBuilder(
          "select null as id, 'pending' as status, null as createdTs, te.next_date as trnDate, 0 as dateNum, " +
              "    tr.from_acc_id as fromAccId, fa.name as fromAccName, " +
              "    tr.to_acc_id as toAccId, ta.name as toAccName, " +
              "    case when fa.type = 'income' then 'income' when ta.type = 'expense' then 'expense' else 'transfer' end as type, " +
              "    null as parentId, te.amount, tr.comment, tr.labels, " +
              "    tr.period, te.id as templId " +
              "  from money_trn_templs te, money_trns tr, accounts fa, accounts ta " +
              "  where te.bs_id = ? and te.status = 'active' " +
              "    and tr.id = te.sample_id and te.next_date < ? " +
              "    and fa.id = tr.from_acc_id and ta.id = tr.to_acc_id ")
      val params = ArrayList<Any>()
      params.add(bsId)
      params.add(beforeDate)
      search.ifPresent { s ->
        sql.append(" and (te.comment ilike ? or te.labels ilike ? or fa.name ilike ? or ta.name ilike ?)")
        val searchTempl = "%$s%"
        for (i in 0..3) {
          params.add(searchTempl)
        }
      }
      sql.append(" order by trnDate desc ")
      return QueryRunner().query(conn, sql.toString(), handler, *params.toArray())
    } catch (e: SQLException) {
      throw HmSqlException(e)
    }
  }

  fun getMoneyTrn(bsId: UUID, id: UUID): MoneyTrn {
    val conn = MainDao.getConnection()
    try {
      return getMoneyTrn(conn, bsId, id)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  fun getMoneyTrn(conn: Connection, bsId: UUID, id: UUID): MoneyTrn {
    val moneyTrn: MoneyTrn
    try {
      val mtHandler = BeanHandler(MoneyTrn::class.java, BasicRowProcessor(MoneyTrnProcessor()))
      return QueryRunner().query(conn,
          baseSelect + " and mt.id = ?",
          mtHandler, bsId, id)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    }
  }

  fun getChildMoneyTrn(conn: Connection, bsId: UUID, id: UUID): MoneyTrn {
    try {
      val mtHandler = BeanHandler(MoneyTrn::class.java, BasicRowProcessor(MoneyTrnProcessor()))
      return QueryRunner().query(conn,
          baseSelect + " and mt.parent_id = ?",
          mtHandler, bsId, id)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    }
  }

  fun createMoneyTrn(bsId: UUID, moneyTrn: MoneyTrn): List<MoneyTrn> {
    val result = ArrayList<MoneyTrn>()
    val conn = MainDao.getConnection()
    try {
      createMoneyTrn(conn, bsId, moneyTrn, result)
      if (moneyTrn.templId != null) {
        val templ = MoneyTrnTemplsDao.getMoneyTrnTempl(conn, bsId, moneyTrn.templId!!)
        templ.nextDate = MoneyTrnTempl.calcNextDate(templ.nextDate!!, templ.period!!)
        MoneyTrnTemplsDao.updateMoneyTrnTempl(bsId, templ)
      }
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
    return result
  }

  @Throws(SQLException::class)
  fun createMoneyTrn(conn: Connection, bsId: UUID, moneyTrn: MoneyTrn) {
    createMoneyTrn(conn, bsId, moneyTrn, ArrayList<MoneyTrn>(0))
  }

  @Throws(SQLException::class)
  private fun createMoneyTrn(conn: Connection, bsId: UUID, moneyTrn: MoneyTrn,
                             result: MutableList<MoneyTrn>?) {
    createMoneyTrnInternal(conn, bsId, moneyTrn)
    val trn1 = getMoneyTrn(conn, bsId, moneyTrn.id!!)
    result?.add(trn1)
    val trn2 = createReserveMoneyTrn(conn, bsId, moneyTrn)
    if (trn2 != null && result != null) {
      result.add(trn2)
    }
    if (done == moneyTrn.status && !moneyTrn.trnDate!!.toLocalDate().isAfter(LocalDate.now())) {
      result!!.forEach { t -> completeMoneyTrn(conn, bsId, t.id!!) }
    }
  }

  @Throws(SQLException::class)
  private fun createReserveMoneyTrn(conn: Connection, balanceSheetId: UUID, moneyTrn: MoneyTrn): MoneyTrn? {
    val bs = MainDao.getBalanceSheet(balanceSheetId)
    val svcRsv = AccountsDao.getAccount(bs.svcRsvId!!)

    var fromAcc = svcRsv
    var account = AccountsDao.getAccount(moneyTrn.fromAccId!!)
    if (Account.Type.debit == account.type) {
      val balance = BalancesDao.getBalance(conn, account.id!!)
      if (balance.reserveId != null) {
        fromAcc = ReservesDao.getReserve(balance.reserveId!!)
      }
    }

    var toAcc = svcRsv
    account = AccountsDao.getAccount(moneyTrn.toAccId!!)
    if (Account.Type.debit == account.type) {
      val balance = BalancesDao.getBalance(conn, account.id!!)
      if (balance.reserveId != null) {
        toAcc = ReservesDao.getReserve(balance.reserveId!!)
      }
    }

    if (fromAcc != toAcc) {
      val rMoneyTrn = MoneyTrn(UUID.randomUUID(), moneyTrn.status!!, moneyTrn.trnDate!!,
          fromAcc.id!!, toAcc.id!!, moneyTrn.amount!!, moneyTrn.period!!, moneyTrn.comment, moneyTrn.labels,
          moneyTrn.dateNum, moneyTrn.id, null, moneyTrn.createdTs)
      createMoneyTrnInternal(conn, balanceSheetId, rMoneyTrn)
      return getMoneyTrn(conn, balanceSheetId, rMoneyTrn.id!!)
    }
    return null
  }

  @Throws(SQLException::class)
  fun createMoneyTrnInternal(conn: Connection, balanceSheetId: UUID, moneyTrn: MoneyTrn) {
    val run = QueryRunner()
    val createdTs = java.sql.Timestamp(java.util.Date().time)

    run.update(conn,
        "insert into money_trns(id, status, balance_sheet_id, created_ts, trn_date, date_num, " +
            "from_acc_id, to_acc_id, parent_id, amount, comment, labels, period, templ_id) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        moneyTrn.id,
        pending.name,
        balanceSheetId,
        createdTs,
        moneyTrn.trnDate,
        0,
        moneyTrn.fromAccId,
        moneyTrn.toAccId,
        moneyTrn.parentId,
        moneyTrn.amount,
        moneyTrn.comment,
        moneyTrn.labelsAsString,
        moneyTrn.period!!.name,
        moneyTrn.templId)
  }

  private fun completeMoneyTrn(conn: Connection, bsId: UUID, moneyTrnId: UUID) {
    try {
      setStatus(conn, bsId, moneyTrnId, done)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    }
  }

  @Throws(SQLException::class)
  private fun setStatus(conn: Connection, bsId: UUID, moneyTrnId: UUID, @NotNull status: Status) {
    val trn = getMoneyTrn(conn, bsId, moneyTrnId)
    if (status == trn.status) {
      return
    }
    var fromAccId = Optional.empty<UUID>()
    var toAccId = Optional.empty<UUID>()
    when (status) {
      done -> if (trn.status == Status.pending || trn.status == Status.cancelled) {
        fromAccId = Optional.of<UUID>(trn.fromAccId)
        toAccId = Optional.of<UUID>(trn.toAccId)
      }
      MoneyTrn.Status.cancelled, pending -> if (trn.status == Status.done) {
        fromAccId = Optional.of<UUID>(trn.toAccId)
        toAccId = Optional.of<UUID>(trn.fromAccId)
      }
      else -> throw HmException(HmException.Code.UnknownMoneyTrnStatus)
    }

    fromAccId.ifPresent { accId ->
      val fromAccount = AccountsDao.getAccount(accId)
      if (!trn.trnDate!!.before(fromAccount.createdDate) && fromAccount.isBalance()) {
        val fromBalance: Balance
        try {
          fromBalance = BalancesDao.getBalance(conn, accId)
          BalancesDao.changeBalanceValue(conn, fromBalance, trn.amount!!.negate())
        } catch (e: SQLException) {
          throw HmSqlException(e)
        }

      }
    }

    toAccId.ifPresent { accId ->
      val toAccount = AccountsDao.getAccount(accId)
      if (!trn.trnDate!!.before(toAccount.createdDate) && toAccount.isBalance()) {
        val toBalance: Balance
        try {
          toBalance = BalancesDao.getBalance(conn, accId)
          BalancesDao.changeBalanceValue(conn, toBalance, trn.amount!!)
        } catch (e: SQLException) {
          throw HmSqlException(e)
        }

      }
    }

    val run = QueryRunner()
    run.update(conn, "update money_trns set status = ? where id = ?",
        status.name, trn.id)
  }

  fun deleteMoneyTrn(bsId: UUID, id: UUID) {
    val conn = MainDao.getConnection()
    try {
      setStatus(conn, bsId, id, Status.cancelled)
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }

  }

  fun updateMoneyTrn(bsId: UUID, moneyTrn: MoneyTrn) {
    val conn = MainDao.getConnection()
    try {
      updateMoneyTrn(conn, bsId, moneyTrn)
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }

  }

  @Throws(SQLException::class)
  private fun updateMoneyTrn(conn: Connection, bsId: UUID, trn: MoneyTrn) {
    val run = QueryRunner()
    val origTrn = getMoneyTrn(conn, bsId, trn.id!!)
    if (trn.crucialEquals(origTrn)) {
      run.update(conn,
          "update money_trns set date_num = ?, period = ?, comment = ?, labels = ? where id = ?",
          trn.dateNum, trn.period!!.name, trn.comment, trn.labelsAsString, trn.id)
    } else {
      val origTrnStatus = trn.status!!
      setStatus(conn, bsId, trn.id!!, Status.cancelled)
      run.update(conn,
          "update money_trns set " +
              " trn_date = ?," +
              " date_num = ?," +
              " from_acc_id = ?," +
              " to_acc_id = ?," +
              " amount = ?," +
              " period = ?, " +
              " comment = ?, " +
              " labels = ? " +
              " where id = ?",
          trn.trnDate,
          trn.dateNum,
          trn.fromAccId,
          trn.toAccId,
          trn.amount,
          trn.period!!.name,
          trn.comment,
          trn.labelsAsString,
          trn.id)
      setStatus(conn, bsId, trn.id!!, origTrnStatus)
    }
  }

  fun skipMoneyTrn(bsId: UUID, trn: MoneyTrn) {
    val conn = MainDao.getConnection()
    try {
      if (trn.id != null) {
        setStatus(conn, bsId, trn.id!!, Status.cancelled)
      }
      if (trn.templId != null) {
        val templ = MoneyTrnTemplsDao.getMoneyTrnTempl(conn, bsId, trn.templId!!)
        templ.nextDate = MoneyTrnTempl.calcNextDate(templ.nextDate!!, templ.period!!)
        MoneyTrnTemplsDao.updateMoneyTrnTempl(bsId, templ)
      }
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }

  }

  fun upMoneyTrn(bsId: UUID, id: UUID) {
    val conn = MainDao.getConnection()
    try {
      val moneyTrn = getMoneyTrn(conn, bsId, id)
      val list = getMoneyTrns(conn, bsId, moneyTrn.trnDate!!)
      val index = list.indexOf(moneyTrn)
      if (index > 0) {
        val prevMoneyTrn = list[index - 1]
        list[index - 1] = moneyTrn
        list[index] = prevMoneyTrn
        val run = QueryRunner()
        var i = 0
        for (m in list) {
          run.update(conn,
              "update money_trns set date_num = ? " + "where balance_sheet_id = ? and id = ?",
              i, bsId, m.id)
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
}