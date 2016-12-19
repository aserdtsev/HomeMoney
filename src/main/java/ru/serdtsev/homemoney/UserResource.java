package ru.serdtsev.homemoney;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import ru.serdtsev.homemoney.dao.MainDao;
import ru.serdtsev.homemoney.dao.UsersDao;
import ru.serdtsev.homemoney.dto.Authentication;
import ru.serdtsev.homemoney.dto.BalanceSheet;
import ru.serdtsev.homemoney.dto.HmResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserResource {
  private Logger log = LoggerFactory.getLogger(getClass());

  @RequestMapping("/balance-sheet-id")
  public HmResponse getBalanceSheetId(
      @CookieValue(value="userId", required=false) UUID userId) {
    HmResponse response;
    try {
      if (userId == null) {
        throw new HmException(HmException.Code.UserIdCookieIsEmpty);
      }
      UUID bsId = UsersDao.getBsId(userId);
      response = HmResponse.getOk(bsId);
    } catch (HmException e) {
      response = HmResponse.getFail(e.getCode());
    }
    return response;
  }

  @RequestMapping(value="/login", method=RequestMethod.POST)
  public HmResponse login(
      @RequestParam("email") String email,
      @RequestParam("pwd") String pwd) {
    try {
      log.info("User login; email:" + email);
      Authentication auth = UsersDao.login(email, pwd);
      return HmResponse.getOk(auth);
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping(value="/logout", method=RequestMethod.POST)
  public HmResponse logout(
      @CookieValue(value="userId", required=false) UUID userId,
      @CookieValue(value="authToken", required=false) UUID authToken) {
    UsersDao.logout(userId, authToken);
    log.info("User logout");
    return HmResponse.getOk();
  }

  @RequestMapping(method = RequestMethod.POST)
  public void createBalanceSheet(BalanceSheet bs) {
    MainDao.INSTANCE.createBalanceSheet(bs.getId());
  }

  @RequestMapping(method = RequestMethod.DELETE)
  public void deleteBalanceSheet(@RequestParam UUID id) {
    MainDao.INSTANCE.deleteBalanceSheet(id);
  }
}
