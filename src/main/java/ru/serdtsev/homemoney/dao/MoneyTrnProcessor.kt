package ru.serdtsev.homemoney.dao

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
        val labels = rs.getString(index)
        if (labels != null)
          return labels.split(",".toRegex()).dropLastWhile { it.isEmpty() }
        else
          return ArrayList<String>()
      }
      else -> return super.processColumn(rs, index, propType)
    }
  }
}
