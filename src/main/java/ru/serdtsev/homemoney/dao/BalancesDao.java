package ru.serdtsev.homemoney.dao;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import ru.serdtsev.homemoney.dto.*;

import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class BalancesDao {
  public static List<Balance> getBalances(UUID bsId) {
    List<Balance> list;
    try (Connection conn = MainDao.getConnection()) {
      list = getBalances(conn, bsId);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return list;
  }

  private static List<Balance> getBalances(Connection conn, UUID bsId) throws SQLException {
    List<Balance> list;ResultSetHandler<List<Balance>> h = new BeanListHandler<>(Balance.class);
    QueryRunner run = new QueryRunner();
    list = run.query(conn,
        "select a.id, a.type, a.name, b.value, a.created_date as createdDate, a.is_arc as arc, b.reserve_id as reserveId," +
              " coalesce(b.credit_limit, 0) as creditLimit, coalesce(b.min_value, 0) as minValue, b.num" +
            " from accounts a, balances b " +
            " where a.balance_sheet_id = ? and a.type in ('debit', 'credit', 'asset') and b.id = a.id " +
            " order by num, a.created_date desc",
        h, bsId);
    return list;
  }

  public static Balance getBalance(Connection conn, UUID id) throws SQLException {
    Balance balance;
    ResultSetHandler<Balance> h = new BeanHandler<>(Balance.class);
    QueryRunner run = new QueryRunner();
    balance = run.query(conn,
        "select a.id, a.type, a.name, b.value, a.created_date as createdDate, a.is_arc as arc, b.reserve_id as reserveId," +
              " coalesce(b.credit_limit, 0) as creditLimit, coalesce(b.min_value, 0) as minValue" +
            " from accounts a, balances b" +
            " where a.id = ? and b.id = a.id",
        h, id);
    return balance;
  }

  public static void createBalance(UUID bsId, Balance balance) {
    try (Connection conn = MainDao.getConnection()) {
      createBalance(conn, bsId, balance);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static void createBalance(Connection conn, UUID bsId, Balance balance) throws SQLException {
    AccountsDao.createAccount(conn, bsId, balance);
    QueryRunner run = new QueryRunner();
    run.update(conn,
        "insert into balances(id, value, reserve_id, credit_limit, min_value) values (?, ?, ?, ?, ?)",
        balance.getId(), balance.getValue(), balance.getReserveId(), balance.getCreditLimit(), balance.getMinValue());
  }

  public static void deleteBalance(UUID bsId, UUID id) {
    try (Connection conn = MainDao.getConnection()) {
      deleteBalance(conn, bsId, id);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  static void deleteBalance(Connection conn, UUID bsId, UUID id) throws SQLException {
    if (!AccountsDao.isTrnExists(conn, id) && !MoneyTrnTemplsDao.isTrnTemplExists(conn, id)) {
      QueryRunner run = new QueryRunner();
      int rows = run.update(conn, "delete from balances where id = ?", id);
      if (rows == 0) {
        throw new IllegalArgumentException("Неверные параметры запроса.");
      }
    }
    AccountsDao.deleteAccount(conn, bsId, id);
  }

  public static void updateBalance(UUID bsId, Balance balance) {
    try (Connection conn = MainDao.getConnection()) {
      updateBalance(conn, bsId, balance);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  static void updateBalance(Connection conn, UUID bsId, Balance balance) throws SQLException {
    AccountsDao.updateAccount(conn, bsId, balance);
    Balance currBalance = getBalance(conn, balance.getId());
    if (balance.getValue().compareTo(currBalance.getValue()) != 0) {
      BalanceSheet bs = MainDao.getBalanceSheet(bsId);
      Boolean more = balance.getValue().compareTo(currBalance.getValue()) > 0;
      UUID fromAccId = more ? bs.uncatIncomeId : balance.getId();
      UUID toAccId = more ? balance.getId() : bs.uncatCostsId;
      BigDecimal amount = balance.getValue().subtract(currBalance.getValue()).abs();
      MoneyTrn moneyTrn = new MoneyTrn(
          UUID.randomUUID(), MoneyTrn.Status.done, java.sql.Date.valueOf(LocalDate.now()), 0, fromAccId, toAccId,
          null, amount, "корректировка остатка", Timestamp.valueOf(LocalDateTime.now()), MoneyTrn.Period.single,
          null, null);
      MoneyTrnsDao.createMoneyTrn(conn, bsId, moneyTrn);
    }

    QueryRunner run = new QueryRunner();
    run.update(conn,
        "update balances set value = ?, reserve_id = ?, credit_limit = ?, min_value = ?" +
            " where id = ?",
        balance.getValue(), balance.getReserveId(), balance.getCreditLimit(),
        balance.getMinValue(), balance.getId());
  }

  public static void upBalance(UUID bsId, Balance balance) {
    try (Connection conn = MainDao.getConnection()) {
      List<Balance> list = getBalances(conn, bsId);
      int index = list.indexOf(balance);
      if (index > 0) {
        Balance prev = list.get(index - 1);
        list.set(index - 1, balance);
        list.set(index, prev);
        QueryRunner run = new QueryRunner();
        int i = 0;
        for (Balance p : list) {
          run.update(conn, "update balances set num = ? where id = ?", i, p.getId());
          i++;
        }
      }
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static void changeBalanceValue(Connection conn, Balance balance, BigDecimal amount) throws SQLException {
    QueryRunner run = new QueryRunner();
    int rows = run.update(conn, "update balances set value = value + ? where id = ?", amount, balance.getId());
    if (rows == 0) {
      throw new IllegalComponentStateException(String.format("Баланс счета {%s} не был изменен в БД.", balance.getId()));
    }
  }
}
