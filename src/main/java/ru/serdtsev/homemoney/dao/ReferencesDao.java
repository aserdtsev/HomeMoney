package ru.serdtsev.homemoney.dao;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.serdtsev.homemoney.dto.HmCurrency;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ReferencesDao {
    private final MainDao mainDao;

    @Autowired
    public ReferencesDao(MainDao mainDao) {
        this.mainDao = mainDao;
    }

    public List<HmCurrency> getCurrencies(UUID bsId) {
    assert Objects.nonNull(bsId);
    try (Connection conn = mainDao.getConnection()) {
      return (new QueryRunner()).query(conn,
          "select b.currency_code " +
              " from accounts a, balances b " +
              " where a.balance_sheet_id = ? and b.id = a.id " +
              " group by currency_code",
          new ColumnListHandler<String>(), bsId)
          .stream()
          .map(code -> {
            Currency currency = Currency.getInstance(code);
            return new HmCurrency(currency.getCurrencyCode(), currency.getDisplayName(), currency.getSymbol());
          })
          .collect(Collectors.toList());
    } catch (SQLException e){
      throw new HmSqlException(e);
    }
  }
}
