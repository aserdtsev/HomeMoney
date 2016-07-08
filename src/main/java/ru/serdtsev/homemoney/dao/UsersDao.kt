package ru.serdtsev.homemoney.dao

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import net.sf.ehcache.Cache
import net.sf.ehcache.CacheManager
import net.sf.ehcache.Element
import org.apache.commons.dbutils.DbUtils
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.BeanHandler
import org.apache.commons.dbutils.handlers.ScalarHandler
import ru.serdtsev.homemoney.HmException
import ru.serdtsev.homemoney.dto.Authentication
import ru.serdtsev.homemoney.dto.User
import java.sql.Connection
import java.sql.SQLException
import java.util.*

object UsersDao {
  fun login(email: String, pwd: String): Authentication {
    val SHARE_SALT = "4301"
    val user: User
    val authToken: UUID = UUID.randomUUID()
    val pwdHash = Hashing.sha1().hashString(pwd + email + SHARE_SALT, Charsets.UTF_8).toString()
    val conn = MainDao.getConnection()
    try {
      user = getUser(conn, email) ?: createUser(conn, email, pwdHash)
      if (user.pwdHash != pwdHash)
        throw HmException(HmException.Code.WrongAuth)
      saveAuthToken(conn, user.userId!!, authToken)
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
    return Authentication(user.userId!!, user.bsId!!, authToken)
  }

  @Throws(SQLException::class)
  private fun getUser(conn: Connection, email: String): User? =
    QueryRunner().query(conn,
        "select user_id as userId, email, pwd_hash as pwdHash, bs_id as bsId from users where email = ?",
        BeanHandler(User::class.java), email)

  fun logout(userId: UUID, authToken: UUID) {
    val conn = MainDao.getConnection()
    try {
      QueryRunner().update(conn, "delete from auth_tokens where user_id = ? and token = ?", userId, authToken)
      DbUtils.commitAndClose(conn)

      val element = authTokensCache.get(userId)
      if (element != null) {
        val authTokens = element.objectValue as MutableSet<*>
        if (authTokens.contains(authToken)) {
          authTokens.remove(authToken)
          if (authTokens.isEmpty()) {
            authTokensCache.remove(userId)
          }
        }
      }
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }

  }

  @Throws(SQLException::class)
  internal fun createUser(conn: Connection, email: String, pwdHash: String): User {
    val bsId = UUID.randomUUID()
    MainDao.createBalanceSheet(conn, bsId)
    QueryRunner().update(conn, "insert into users(user_id, email, pwd_hash, bs_id) values (?, ?, ?, ?)",
        UUID.randomUUID(), email, pwdHash, bsId)
    return getUser(conn, email)!!
  }

  @Throws(SQLException::class)
  private fun saveAuthToken(conn: Connection, userId: UUID, authToken: UUID) {
    QueryRunner().update(conn, "insert into auth_tokens(user_id, token) values (?, ?)", userId, authToken)
  }

  fun checkAuthToken(userId: UUID, authToken: UUID) {
    val element = authTokensCache.get(userId)
    if (element != null) {
      val authTokens = element.objectValue as Set<*>
      if (authTokens.contains(authToken)) {
        return
      }
    }

    val conn = MainDao.getConnection()
    try {
      checkAuthToken(conn, userId, authToken)
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }

  }

  @Suppress("unchecked", "UNCHECKED_CAST")
  @Throws(SQLException::class)
  private fun checkAuthToken(conn: Connection, userId: UUID, authToken: UUID) {
    val tokenNum = QueryRunner().query(conn,
        "select count(1) from auth_tokens where user_id = ? and token = ?",
        ScalarHandler<Long>(1), userId, authToken)
    if (tokenNum == 0L) {
      throw HmException(HmException.Code.WrongAuth)
    }

    val element = authTokensCache.get(userId) ?: Element(userId, HashSet<UUID>())
    val authTokens = element.objectValue as MutableSet<UUID>
    if (authTokens.isEmpty())
      authTokensCache.put(element)
    authTokens.add(authToken)
  }

  fun getBsId(userId: UUID): UUID {
    val conn = MainDao.getConnection()
    try {
      return getBsId(conn, userId)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
  }

  @Throws(SQLException::class)
  private fun getBsId(conn: Connection, userId: UUID) =
    QueryRunner().query(conn, "select bs_id from users where user_id = ?", ScalarHandler<UUID>(1), userId)

  private val authTokensCache: Cache
    get() = CacheManager.getInstance().getCache("authTokens")

}
