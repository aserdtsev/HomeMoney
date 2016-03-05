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
    var user: User?
    val authToken: UUID = UUID.randomUUID()
    val pwdHash = Hashing.sha1().hashString(pwd + email + SHARE_SALT, Charsets.UTF_8).toString()
    val conn = MainDao.getConnection()
    try {
      user = getUser(conn, email)
      if (user != null) {
        if (user.pwdHash != pwdHash) {
          throw HmException(HmException.Code.AuthWrong)
        }
      } else {
        user = createUser(conn, email, pwdHash)
      }
      saveAuthToken(conn, user.userId!!, authToken)
      DbUtils.commitAndClose(conn)
    } catch (e: SQLException) {
      throw HmSqlException(e)
    } finally {
      DbUtils.close(conn)
    }
    return Authentication(user?.userId!!, user?.bsId!!, authToken)
  }

  @Throws(SQLException::class)
  private fun getUser(conn: Connection, email: String): User? {
    val user: User
    val run = QueryRunner()
    val h = BeanHandler(User::class.java)
    user = run.query(conn,
        "select user_id as userId, email, pwd_hash as pwdHash, bs_id as bsId from users where email = ?",
        h, email)
    return user
  }

  fun logout(userId: UUID, authToken: UUID) {
    val conn = MainDao.getConnection()
    try {
      val run = QueryRunner()
      run.update(conn, "delete from auth_tokens where user_id = ? and token = ?", userId, authToken)
      DbUtils.commitAndClose(conn)

      val cache = authTokensCache
      val element = cache.get(userId)
      val authTokens: Set<*>
      if (element != null) {
        authTokens = element.objectValue as Set<*>
        if (authTokens.contains(authToken)) {
          authTokens.minus(authToken)
          if (authTokens.isEmpty()) {
            cache.remove(userId)
          } else {
            cache.put(element)
          }
        }
      }
    } catch (e: SQLException) {
      throw HmSqlException(e)
    }

  }

  @Throws(SQLException::class)
  internal fun createUser(conn: Connection, email: String, pwdHash: String): User {
    val run = QueryRunner()
    val bsId: UUID = UUID.randomUUID()
    MainDao.createBalanceSheet(conn, bsId)
    run.update(conn, "insert into users(user_id, email, pwd_hash, bs_id) values (?, ?, ?, ?)",
        UUID.randomUUID(), email, pwdHash, bsId)
    return getUser(conn, email)!!
  }

  @Throws(SQLException::class)
  private fun saveAuthToken(conn: Connection, userId: UUID, authToken: UUID) {
    val run = QueryRunner()
    run.update(conn, "insert into auth_tokens(user_id, token) values (?, ?)", userId, authToken)
  }

  fun checkAuthToken(userId: UUID, authToken: UUID) {
    val cache = authTokensCache
    val element = cache.get(userId)
    if (element != null) {
      //noinspection unchecked
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

  @Throws(SQLException::class)
  private fun checkAuthToken(conn: Connection, userId: UUID, authToken: UUID) {
    val run = QueryRunner()
    val countHandler = ScalarHandler<Long>(1)
    val tokenNum: Long = run.query(conn,
        "select count(1) from auth_tokens where user_id = ? and token = ?",
        countHandler, userId, authToken)
    if (tokenNum == 0L) {
      throw HmException(HmException.Code.AuthWrong)
    }

    val cache = authTokensCache
    val element = cache.get(userId)
    val authTokens: Set<*>
    if (element != null) {
      //noinspection unchecked
      authTokens = element.objectValue as Set<*>
      if (!authTokens.contains(authToken)) {
        authTokens.plus(authToken)
        cache.put(element)
      }
    } else {
      authTokens = HashSet<UUID>()
      authTokens.add(authToken)
      cache.put(Element(userId, authTokens))
    }
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
  private fun getBsId(conn: Connection, userId: UUID): UUID {
    val run = QueryRunner()
    val uuidHandler = ScalarHandler<UUID>(1)
    return run.query(conn, "select bs_id from users where user_id = ?", uuidHandler, userId)
  }

  private val authTokensCache: Cache
    get() = CacheManager.getInstance().getCache("authTokens")

}
