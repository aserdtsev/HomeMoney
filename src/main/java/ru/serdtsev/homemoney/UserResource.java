package ru.serdtsev.homemoney;

import ru.serdtsev.homemoney.dao.MainDao;
import ru.serdtsev.homemoney.dao.UsersDao;
import ru.serdtsev.homemoney.dto.Authentication;
import ru.serdtsev.homemoney.dto.BalanceSheet;
import ru.serdtsev.homemoney.dto.HmResponse;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/user")
public class UserResource {
  @POST
  @Path("/login")
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse login(
      @QueryParam("email") String email,
      @QueryParam("pwd") String pwd) {
    try {
      Authentication auth = UsersDao.login(email, pwd);
      return HmResponse.Companion.getOk(auth);
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }

  @GET
  @Path("/balance-sheet-id")
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse getBalanceSheetId(@CookieParam("userId") UUID userId) {
    try {
      UUID bsId = UsersDao.getBsId(userId);
      return HmResponse.Companion.getOk(bsId);
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }

  @POST
  @Path("/logout")
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse logout(
      @CookieParam("userId") UUID userId,
      @CookieParam("authToken") UUID authToken) {
    UsersDao.logout(userId, authToken);
    return HmResponse.Companion.getOk();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public void createBalanceSheet(BalanceSheet balanceSheet) {
    MainDao.createBalanceSheet(balanceSheet.id);
  }

  @DELETE
  public void deleteBalanceSheet(@QueryParam("id") String id) {
    MainDao.deleteBalanceSheet(UUID.fromString(id));
  }
}
