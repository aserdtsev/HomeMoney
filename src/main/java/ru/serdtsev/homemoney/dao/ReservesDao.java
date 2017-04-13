package ru.serdtsev.homemoney.dao;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.serdtsev.homemoney.dto.ReserveDto;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Component
public class ReservesDao {
  private BalancesDao balancesDao;

  @Autowired
  public ReservesDao(BalancesDao balancesDao) {
    this.balancesDao = balancesDao;
  }

  public static List<ReserveDto> getReserves(UUID bsId) {
    try (Connection conn = MainDao.getConnection()) {
      return (new QueryRunner()).query(conn,
          "select a.id, a.name, a.type, b.value, a.created_date as createdDate, a.is_arc as isArc, b.currency_code as currencyCode, r.target " +
              "from accounts a, balances b, reserves r " +
              "where a.balance_sheet_id = ? and b.id = a.id and r.id = a.id " +
              "order by a.created_date desc",
          new BeanListHandler<>(ReserveDto.class), bsId);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }
}
