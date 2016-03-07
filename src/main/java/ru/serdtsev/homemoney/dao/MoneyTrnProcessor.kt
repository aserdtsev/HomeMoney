package ru.serdtsev.homemoney.dao

import com.google.common.base.Strings
import org.apache.commons.dbutils.BeanProcessor

import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

class MoneyTrnProcessor : BeanProcessor() {
  @Throws(SQLException::class)
  override fun processColumn(rs: ResultSet, index: Int, propType: Class<*>): Any? {
    val columnName = rs.metaData.getColumnName(index)
    when (columnName) {
      "labels" -> {
        val result = ArrayList<String>()
        val labels = Optional.ofNullable<String>(Strings.emptyToNull(rs.getString(index)))
        labels.ifPresent { s -> Collections.addAll(result, *s.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) }
        return result
      }
      else -> return super.processColumn(rs, index, propType)
    }
  }
}
