package ru.serdtsev.homemoney.dao;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import ru.serdtsev.homemoney.dto.Account;
import ru.serdtsev.homemoney.dto.Category;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class CategoriesDao {
  public static List<Category> getCategories(UUID bsId) {
    try (Connection conn = MainDao.getConnection()) {
      return (new QueryRunner()).query(conn,
          "select a.id, a.name, a.type, a.is_arc as isArc, c.root_id as rootId, " +
              "case when c.root_id is null then a.name " +
              "else (select name from accounts where id = c.root_id) || '#' || a.name end as sort " +
              "from accounts a, categories c " +
              "where a.balance_sheet_id = ? and a.type in ('income', 'expense') " +
              "and c.id = a.id " +
              "order by type, sort",
          new BeanListHandler<>(Category.class), bsId);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  @SuppressWarnings("unused")
  public static Category getCategory(UUID id) {
    try (Connection conn = MainDao.getConnection()) {
      return (new QueryRunner()).query(conn,
          "select a.id, a.name, a.type, c.root_id as rootId " + "from accounts a, categories c where id = ? and c.id = a.id",
          new BeanHandler<>(Category.class), id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static void createCategory(UUID bsId, Category category) {
    try (Connection conn = MainDao.getConnection()) {
      Account account = new Account(category.getId(), category.getType(), category.getName());
      AccountsDao.createAccount(conn, bsId, account);
      (new QueryRunner()).update(conn,
          "insert into categories (id, root_id) values (?, ?)",
          category.getId(), category.getRootId());
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static void deleteCategory(UUID bsId, UUID id) {
    try (Connection conn = MainDao.getConnection()) {
      if (!AccountsDao.isTrnExists(conn, id) && !MoneyTrnTemplsDao.isTrnTemplExists(conn, id)) {
        (new QueryRunner()).update(conn, "delete from categories where id = ?", id);
      }
      AccountsDao.deleteAccount(conn, bsId, id);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static void updateCategory(UUID bsId, Category category) {
    try (Connection conn = MainDao.getConnection()) {
      AccountsDao.updateAccount(conn, bsId, category);
      (new QueryRunner()).update(conn,
          "update categories set root_id = ? where id = ?",
          category.getRootId(), category.getId());
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }}
