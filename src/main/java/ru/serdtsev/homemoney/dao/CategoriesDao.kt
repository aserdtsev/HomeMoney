package ru.serdtsev.homemoney.dao

import org.apache.commons.dbutils.DbUtils
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.BeanHandler
import org.apache.commons.dbutils.handlers.BeanListHandler
import ru.serdtsev.homemoney.dto.Account
import ru.serdtsev.homemoney.dto.Category
import java.sql.SQLException
import java.util.*

object CategoriesDao {
  fun getCategories(bsId: UUID): List<Category> {
    val conn = MainDao.getConnection()
    try {
      return QueryRunner().query(conn,
          "select a.id, a.name, a.type, a.is_arc as arc, c.root_id as rootId, " +
              "case when c.root_id is null then a.name " +
              "else (select name from accounts where id = c.root_id) || '#' || a.name end as sort " +
              "from accounts a, categories c " +
              "where a.balance_sheet_id = ? and a.type in ('income', 'expense') " +
              "and c.id = a.id " +
              "order by type, sort",
          BeanListHandler(Category::class.java), bsId)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  fun getCategory(id: UUID): Category {
    val conn = MainDao.getConnection()
    try {
      return QueryRunner().query(conn,
          "select a.id, a.name, a.type, c.root_id as rootId " + "from accounts a, categories c where id = ? and c.id = a.id",
          BeanHandler(Category::class.java), id)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  fun createCategory(balanceSheetId: UUID, category: Category) {
    val conn = MainDao.getConnection()
    try {
      val account = Account(category)
      AccountsDao.createAccount(conn, balanceSheetId, account)
      QueryRunner().update(conn,
          "insert into categories (id, root_id) values (?, ?)",
          category.id, category.rootId)
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  fun deleteCategory(bsId: UUID, id: UUID) {
    val conn = MainDao.getConnection()
    try {
      if (!AccountsDao.isTrnExists(conn, id) && !MoneyTrnTemplsDao.isTrnTemplExists(conn, id)) {
        val run = QueryRunner()
        run.update(conn, "delete from categories where id = ?", id)
      }
      AccountsDao.deleteAccount(conn, bsId, id)
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  fun updateCategory(bsId: UUID, category: Category) {
    val conn = MainDao.getConnection()
    try {
      AccountsDao.updateAccount(conn, bsId, category)
      val run = QueryRunner()
      run.update(conn,
          "update categories set root_id = ? where id = ?",
          category.rootId, category.id)
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }
}
