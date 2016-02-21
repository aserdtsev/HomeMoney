package ru.serdtsev.homemoney.dao;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
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
    List<Category> list;
    try (Connection conn = MainDao.getConnection()) {
      ResultSetHandler<List<Category>> h = new BeanListHandler<>(Category.class);
      QueryRunner run = new QueryRunner();
      list = run.query(conn,
          "select a.id, a.name, a.type, a.is_arc as isArc, c.root_id as rootId, " +
                "case when c.root_id is null then a.name " +
                  "else (select name from accounts where id = c.root_id) || '#' || a.name end as sort " +
              "from accounts a, categories c " +
              "where a.balance_sheet_id = ? and a.type in ('income', 'expense') " +
                "and c.id = a.id " +
              "order by type, sort",
          h, bsId);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return list;
  }

  public static Category getCategory(UUID id) {
    Category category;
    try (Connection conn = MainDao.getConnection()) {
      ResultSetHandler<Category> h = new BeanHandler<>(Category.class);
      QueryRunner run = new QueryRunner();
      category = run.query(conn,
          "select a.id, a.name, a.type, c.root_id as rootId " +
              "from accounts a, categories c where id = ? and c.id = a.id",
          h, id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return category;
  }

  public static void createCategory(UUID balanceSheetId, Category category) {
    try (Connection conn = MainDao.getConnection()) {
      Account account = new Account(category);
      AccountsDao.createAccount(conn, balanceSheetId, account);
      QueryRunner run = new QueryRunner();
      run.update(conn,
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
        QueryRunner run = new QueryRunner();
        run.update(conn, "delete from categories where id = ?", id);
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
      QueryRunner run = new QueryRunner();
      run.update(conn,
          "update categories set root_id = ? where id = ?",
          category.getRootId(), category.getId());
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }
}
