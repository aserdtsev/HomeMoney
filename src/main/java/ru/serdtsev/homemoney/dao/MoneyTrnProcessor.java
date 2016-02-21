package ru.serdtsev.homemoney.dao;

import com.google.common.base.Strings;
import org.apache.commons.dbutils.BeanProcessor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MoneyTrnProcessor extends BeanProcessor {
  @Override
  protected Object processColumn(ResultSet rs, int index, Class<?> propType) throws SQLException {
    String columnName = rs.getMetaData().getColumnName(index);
    switch(columnName) {
      case "labels":
        List<String> result = new ArrayList<>();
        Optional<String> labels = Optional.ofNullable(Strings.emptyToNull(rs.getString(index)));
        labels.ifPresent(s -> Collections.addAll(result, s.split(",")));
        return result;
      default:
        return super.processColumn(rs, index, propType);
    }
  }
}
