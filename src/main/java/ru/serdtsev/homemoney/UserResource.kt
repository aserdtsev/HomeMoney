package ru.serdtsev.homemoney;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.common.HmException;
import ru.serdtsev.homemoney.common.HmResponse;
import ru.serdtsev.homemoney.user.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserResource {
  private static Logger log = LoggerFactory.getLogger(UserResource.class);
  private static final String SHARE_SALT = "4301";

  private UserRepository userRepo;
  private UserAuthTokenRepository userAuthTokenRepo;
  private BalanceSheetRepository balanceSheetRepo;

  @Autowired
  public UserResource(UserRepository userRepo, UserAuthTokenRepository userAuthTokenRepo) {
    this.userRepo = userRepo;
    this.userAuthTokenRepo = userAuthTokenRepo;
  }

  @RequestMapping("/balance-sheet-id")
  @Transactional(readOnly = true)
  public HmResponse getBalanceSheetId(
      @CookieValue(value="userId", required=false) UUID userId) {
    HmResponse response;
    try {
      if (userId == null) {
        throw new HmException(HmException.Code.UserIdCookieIsEmpty);
      }
      User user = userRepo.findById(userId).get();
      response = HmResponse.getOk(user.getBsId());
    } catch (HmException e) {
      response = HmResponse.getFail(e.getCode());
    }
    return response;
  }

  @RequestMapping(value="/login", method=RequestMethod.POST)
  @Transactional
  public HmResponse login(
      @RequestParam("email") String email,
      @RequestParam("pwd") String pwd) {
    try {
      log.info("User login; email:" + email);

      final String pwdHash = Hashing.sha1().hashString(pwd + email + SHARE_SALT, Charsets.UTF_8).toString();
      User user = userRepo.findByEmail(email);
      if (user == null) {
        user = createUserNBalanceSheet(email, pwdHash);
      }

      if (!user.getPwdHash().equals(pwdHash))
        throw new HmException(HmException.Code.WrongAuth);

      final UUID authToken = UUID.randomUUID();
      saveUserAuthToken(authToken, user);

      Authentication auth = new Authentication(user.getId(), user.getBsId(), authToken);
      return HmResponse.getOk(auth);
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping(value="/logout", method=RequestMethod.POST)
  @Transactional
  public HmResponse logout(
      @CookieValue(value="userId", required=false) UUID userId,
      @CookieValue(value="authToken", required=false) UUID authToken) {
    UserAuthToken userAuthToken = userAuthTokenRepo.findById(authToken).get();
    if (!userAuthToken.getUserId().equals(userId)) {
      throw new HmException(HmException.Code.WrongUserId);
    }
    userAuthTokenRepo.delete(userAuthToken);

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

    log.info("User logout");
    return HmResponse.getOk();
  }

  @RequestMapping(method = RequestMethod.DELETE)
  @Transactional
  public void deleteBalanceSheet(@RequestParam UUID id) {
    balanceSheetRepo.deleteById(id);
  }

  User createUserNBalanceSheet(String email, String pwdHash) {
    BalanceSheet bs = BalanceSheet.Companion.newInstance();
    balanceSheetRepo.save(bs);
    User user = new User(UUID.randomUUID(), bs.getId(), email, pwdHash);
    userRepo.save(user);
    return user;
  }

  private void saveUserAuthToken(UUID authToken, User user) {
    UserAuthToken userAuthToken = new UserAuthToken(authToken, user.getId());
    userAuthTokenRepo.save(userAuthToken);
    log.info("User is logged; userId:{}.", user.getId());
  }

  private Cache getAuthTokensCache() {
    return CacheManager.getInstance().getCache("authTokens");
  }

}
