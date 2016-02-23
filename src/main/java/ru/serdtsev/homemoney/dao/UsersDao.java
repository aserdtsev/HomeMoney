package ru.serdtsev.homemoney.dao;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import ru.serdtsev.homemoney.HmException;
import ru.serdtsev.homemoney.dto.Authentication;
import ru.serdtsev.homemoney.dto.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class UsersDao {
  public static Authentication login(String email, String pwd) {
    final String SHARE_SALT = "4301";
    User user;
    UUID authToken = UUID.randomUUID();
    try (Connection conn = MainDao.getConnection()) {
      user = getUser(conn, email);
      String pwdHash = Hashing.sha1().hashString(pwd + email + SHARE_SALT, Charsets.UTF_8).toString();
      if (user != null) {
        if (!user.getPwdHash().equals(pwdHash)) {
          throw new HmException(HmException.Code.AuthWrong);
        }
      } else {
        user = createUser(conn, email, pwdHash);
      }
      saveAuthToken(conn, user.getUserId(), authToken);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return new Authentication(user.getUserId(), user.getBsId(), authToken);
  }

  private static User getUser(Connection conn, String email) throws SQLException {
    User user;QueryRunner run = new QueryRunner();
    ResultSetHandler<User> h = new BeanHandler<>(User.class);
    user = run.query(conn,
        "select user_id as userId, email, pwd_hash as pwdHash, bs_id as bsId from users where email = ?",
        h, email);
    return user;
  }

  public static void logout(UUID userId, UUID authToken) {
    try (Connection conn = MainDao.getConnection()) {
      QueryRunner run = new QueryRunner();
      run.update(conn, "delete from auth_tokens where user_id = ? and token = ?", userId, authToken);
      DbUtils.commitAndClose(conn);

      Cache cache = getAuthTokensCache();
      Element element = cache.get(userId);
      Set<UUID> authTokens;
      if (element != null) {
        //noinspection unchecked
        authTokens = (Set<UUID>)element.getObjectValue();
        if (authTokens.contains(authToken)) {
          authTokens.remove(authToken);
          if (authTokens.isEmpty()) {
            cache.remove(userId);
          } else {
            cache.put(element);
          }
        }
      }
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  static User createUser(Connection conn, String email, String pwdHash) throws SQLException {
    QueryRunner run = new QueryRunner();
    UUID bsId;
    bsId = UUID.randomUUID();
    MainDao.createBalanceSheet(conn, bsId);
    run.update(conn, "insert into users(user_id, email, pwd_hash, bs_id) values (?, ?, ?, ?)",
        UUID.randomUUID(), email, pwdHash, bsId);
    return getUser(conn, email);
  }

  static private void saveAuthToken(Connection conn, UUID userId, UUID authToken) throws SQLException {
    QueryRunner run = new QueryRunner();
    run. update(conn, "insert into auth_tokens(user_id, token) values (?, ?)", userId, authToken);
  }

  static public void checkAuthToken(UUID userId, UUID authToken) {
    Cache cache = getAuthTokensCache();
    Element element = cache.get(userId);
    if (element != null) {
      //noinspection unchecked
      Set<UUID> authTokens = (Set<UUID>)element.getObjectValue();
      if (authTokens.contains(authToken)) {
        return;
      }
    }

    try (Connection conn = MainDao.getConnection()) {
      checkAuthToken(conn, userId, authToken);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  static private void checkAuthToken(Connection conn, UUID userId, UUID authToken) throws SQLException {
    QueryRunner run = new QueryRunner();
    ResultSetHandler<Long> countHandler = new ScalarHandler<>(1);
    long tokenNum = run.query(conn,
        "select count(1) from auth_tokens where user_id = ? and token = ?",
        countHandler, userId, authToken);
    if (tokenNum == 0) {
      throw new HmException(HmException.Code.AuthWrong);
    }

    Cache cache = getAuthTokensCache();
    Element element = cache.get(userId);
    Set<UUID> authTokens;
    if (element != null) {
      //noinspection unchecked
      authTokens = (Set<UUID>)element.getObjectValue();
      if (!authTokens.contains(authToken)) {
        authTokens.add(authToken);
        cache.put(element);
      }
    } else {
      authTokens = new HashSet<>();
      authTokens.add(authToken);
      cache.put(new Element(userId, authTokens));
    }
  }

  static public UUID getBsId(UUID userId) {
    try (Connection conn = MainDao.getConnection()) {
      return getBsId(conn, userId);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  static private UUID getBsId(Connection conn, UUID userId) throws SQLException {
    QueryRunner run = new QueryRunner();
    ResultSetHandler<UUID> uuidHandler = new ScalarHandler<>(1);
    return run.query(conn, "select bs_id from users where user_id = ?", uuidHandler, userId);
  }

  static private Cache getAuthTokensCache() {
    return CacheManager.getInstance().getCache("authTokens");
  }

}
