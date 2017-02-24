package ru.serdtsev.homemoney.dao;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import ru.serdtsev.homemoney.HmException;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.dto.Authentication;
import ru.serdtsev.homemoney.dto.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class UsersDao {
  private static Logger log = org.slf4j.LoggerFactory.getLogger(UsersDao.class);

  public static Authentication login(String email, String pwd) {
    final String SHARE_SALT = "4301";
    User user;
    UUID authToken = UUID.randomUUID();
    String pwdHash = Hashing.sha1().hashString(pwd + email + SHARE_SALT, Charsets.UTF_8).toString();
    try (Connection conn = MainDao.getConnection()) {
      user = getUser(conn, email);
      if (user == null) {
        user = createUser(conn, email, pwdHash);
      }
      if (!user.getPwdHash().equals(pwdHash))
        throw new HmException(HmException.Code.WrongAuth);
      saveAuthToken(conn, user.getUserId(), authToken);
      log.info("User is logged; userId:{}.", user.getUserId());
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
    return new Authentication(user.getUserId(), user.getBsId(), authToken);
  }

  private static User getUser(Connection conn, String email) throws SQLException {
    return (new QueryRunner()).query(conn,
        "select user_id as userId, email, pwd_hash as pwdHash, bs_id as bsId from users where email = ?",
        (new BeanHandler<>(User.class)), email);
  }

  public static void logout(UUID userId, UUID authToken) {
    try (Connection conn = MainDao.getConnection()) {
      (new QueryRunner()).update(conn, "delete from auth_tokens where user_id = ? and token = ?", userId, authToken);
      DbUtils.commitAndClose(conn);

      Cache authTokensCache = getAuthTokensCache();
      Element element = authTokensCache.get(userId);
      if (element != null) {
        Set authTokens = (Set)element.getObjectValue();
        if (authTokens.contains(authToken)) {
          authTokens.remove(authToken);
          if (authTokens.isEmpty()) {
            authTokensCache.remove(userId);
          }
        }
      }
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private static User createUser(Connection conn, String email, String pwdHash) throws SQLException {
    BalanceSheet bs = BalanceSheet.newInstance();
    (new QueryRunner()).update(conn, "insert into users(user_id, email, pwd_hash, bs_id) values (?, ?, ?, ?)",
        UUID.randomUUID(), email, pwdHash, bs.getId());
    return getUser(conn, email);
  }

  private static void saveAuthToken(Connection conn, UUID userId, UUID authToken) throws SQLException {
    (new QueryRunner()).update(conn, "insert into auth_tokens(user_id, token) values (?, ?)", userId, authToken);
  }

  public static void checkAuthToken(UUID userId, UUID authToken) {
    Cache authTokensCache = getAuthTokensCache();
    Element element = authTokensCache.get(userId);
    if (element != null) {
      Set authTokens = (Set)element.getObjectValue();
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

  private static void checkAuthToken(Connection conn, UUID userId, UUID authToken) throws SQLException {
    Long tokenNum = (new QueryRunner()).query(conn,
        "select count(1) from auth_tokens where user_id = ? and token = ?",
        new ScalarHandler<Long>(1), userId, authToken);
    if (tokenNum == 0L) {
      throw new HmException(HmException.Code.WrongAuth);
    }

    Cache authTokensCache = getAuthTokensCache();
    Element element = authTokensCache.get(userId);
    if (element == null) {
      element = new Element(userId, new HashSet<UUID>());
    }
    @SuppressWarnings("unchecked") Set<UUID> authTokens = (Set<UUID>) element.getObjectValue();
    if (authTokens.isEmpty())
      authTokensCache.put(element);
    authTokens.add(authToken);
  }

  public static UUID getBsId(UUID userId) {
    try (Connection conn = MainDao.getConnection()) {
      return getBsId(conn, userId);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private static UUID getBsId(Connection conn, UUID userId) throws SQLException {
    return (new QueryRunner()).query(conn, "select bs_id from users where user_id = ?",
        new ScalarHandler<UUID>(1), userId);
  }

  private static Cache getAuthTokensCache() {
    return CacheManager.getInstance().getCache("authTokens");
  }

}
