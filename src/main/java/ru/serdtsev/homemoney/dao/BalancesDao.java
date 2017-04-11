package ru.serdtsev.homemoney.dao;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.serdtsev.homemoney.account.BalanceDto;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.dto.MoneyTrn;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class BalancesDao {
  private AccountsDao accountsDao;
  private BalanceSheetRepository balanceSheetRepo;

  @Autowired
  public BalancesDao(AccountsDao accountsDao, BalanceSheetRepository balanceSheetRepo) {
    this.accountsDao = accountsDao;
    this.balanceSheetRepo = balanceSheetRepo;
  }

  BalanceDto getBalance(Connection conn, UUID id) throws SQLException {
    return (new QueryRunner()).query(conn,
        "select a.id, a.type, a.name, b.currency_code as currencyCode, b.value, a.created_date as createdDate, " +
            " a.is_arc as isArc, b.reserve_id as reserveId," +
            " coalesce(b.credit_limit, 0) as creditLimit, coalesce(b.min_value, 0) as minValue" +
            " from accounts a, balances b" +
            " where a.id = ? and b.id = a.id",
        new BeanHandler<>(BalanceDto.class), id);
  }

  void createBalance(Connection conn, UUID bsId, BalanceDto balance) throws SQLException {
    AccountsDao.createAccount(conn, bsId, balance);
    (new QueryRunner()).update(conn,
        "insert into balances(id, currency_code, value, reserve_id, credit_limit, min_value) values (?, ?, ?, ?, ?, ?)",
        balance.getId(), balance.getCurrencyCode(), balance.getValue(), balance.getReserveId(), balance.getCreditLimit(),
        balance.getMinValue());
  }

  void deleteBalance(Connection conn, UUID bsId, UUID id) throws SQLException {
    if (!AccountsDao.isTrnExists(conn, id) && !MoneyTrnTemplsDao.isTrnTemplExists(conn, id)) {
      int rows = (new QueryRunner()).update(conn, "delete from balances where id = ?", id);
      if (rows == 0) {
        throw new IllegalArgumentException("Неверные параметры запроса.");
      }
    }
    accountsDao.deleteAccount(conn, bsId, id);
  }

  MoneyTrn updateBalance(Connection conn, UUID bsId, BalanceDto balance) throws SQLException {
    AccountsDao.updateAccount(conn, bsId, balance);
    BalanceDto currBalance = getBalance(conn, balance.getId());
    MoneyTrn moneyTrn = null;
    if (balance.getValue().compareTo(currBalance.getValue()) != 0) {
      BalanceSheet bs = balanceSheetRepo.findOne(bsId);
      boolean more = balance.getValue().compareTo(currBalance.getValue()) == 1;
      UUID fromAccId = more ? bs.getUncatIncome().getId() : balance.getId();
      UUID toAccId = more ? balance.getId() : bs.getUncatCosts().getId();
      BigDecimal amount = balance.getValue().subtract(currBalance.getValue()).abs();
      moneyTrn = new MoneyTrn(UUID.randomUUID(), MoneyTrn.Status.done, java.sql.Date.valueOf(LocalDate.now()),
          fromAccId, toAccId, amount, MoneyTrn.Period.single, "корректировка остатка");
    }

    (new QueryRunner()).update(conn,
        "update balances set value = ?, reserve_id = ?, credit_limit = ?, min_value = ?" + " where id = ?",
        balance.getValue(), balance.getReserveId(), balance.getCreditLimit(), balance.getMinValue(), balance.getId());
    return moneyTrn;
  }
}
