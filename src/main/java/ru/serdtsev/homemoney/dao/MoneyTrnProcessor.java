package ru.serdtsev.homemoney.dao;

import com.google.common.base.Strings;
import org.apache.commons.dbutils.BeanProcessor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

public class MoneyTrnProcessor extends BeanProcessor {
  @Override
  public Object processColumn(ResultSet rs, int index, Class<?> propType) throws SQLException {
    String columnName = rs.getMetaData().getColumnName(index);
    switch (columnName) {
      case "labels":
        String labels = rs.getString(index);
        if (!Strings.isNullOrEmpty(labels))
          return Arrays.asList(labels.split(","));
        else
          return new ArrayList<String>();
      default:
        return super.processColumn(rs, index, propType);
    }
  }
}
