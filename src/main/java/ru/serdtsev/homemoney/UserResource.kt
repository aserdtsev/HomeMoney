package ru.serdtsev.homemoney

import ru.serdtsev.homemoney.dao.MainDao
import ru.serdtsev.homemoney.dao.UsersDao
import ru.serdtsev.homemoney.dto.BalanceSheet
import ru.serdtsev.homemoney.dto.HmResponse
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/user")
class UserResource {
  @POST
  @Path("/login")
  @Produces(MediaType.APPLICATION_JSON)
  fun login(
      @QueryParam("email") email: String,
      @QueryParam("pwd") pwd: String): HmResponse {
    try {
      val auth = UsersDao.login(email, pwd)
      return HmResponse.getOk(auth)
    } catch (e: HmException) {
      return HmResponse.getFail(e.code)
    }

  }

  @GET
  @Path("/balance-sheet-id")
  @Produces(MediaType.APPLICATION_JSON)
  fun getBalanceSheetId(@CookieParam("userId") userId: UUID): HmResponse {
    try {
      val bsId = UsersDao.getBsId(userId)
      return HmResponse.getOk(bsId)
    } catch (e: HmException) {
      return HmResponse.getFail(e.code)
    }

  }

  @POST
  @Path("/logout")
  @Produces(MediaType.APPLICATION_JSON)
  fun logout(
      @CookieParam("userId") userId: UUID,
      @CookieParam("authToken") authToken: UUID): HmResponse {
    UsersDao.logout(userId, authToken)
    return HmResponse.ok
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  fun createBalanceSheet(balanceSheet: BalanceSheet) {
    MainDao.createBalanceSheet(balanceSheet.id)
  }

  @DELETE
  fun deleteBalanceSheet(@QueryParam("id") id: UUID) {
    MainDao.deleteBalanceSheet(id)
  }
}
