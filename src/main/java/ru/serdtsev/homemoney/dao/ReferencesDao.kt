package ru.serdtsev.homemoney.dao

import org.apache.commons.dbutils.DbUtils
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.ColumnListHandler
import ru.serdtsev.homemoney.dto.Currency
import java.sql.SQLException
import java.util.*

object ReferencesDao {
  fun getCurrencies(bsId: UUID): List<Currency> {
    val conn = MainDao.getConnection()
    return try {
      val currencyCodes = QueryRunner().query(conn,
          "select b.currency_code " +
              " from accounts a, balances b " +
              " where a.balance_sheet_id = ? and b.id = a.id " +
              " group by currency_code",
          ColumnListHandler<String>(), bsId)
      currencyCodes.map { code -> java.util.Currency.getInstance(code) }
        .map { c -> Currency(c.currencyCode, c.displayName, c.symbol) }
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }
}