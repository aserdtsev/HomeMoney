package ru.serdtsev.homemoney

import org.slf4j.LoggerFactory
import ru.serdtsev.homemoney.dao.MainDao
import ru.serdtsev.homemoney.dao.UsersDao
import ru.serdtsev.homemoney.dto.BalanceSheet
import ru.serdtsev.homemoney.dto.HmResponse
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/user")
class UserResource {
  private val log = LoggerFactory.getLogger(javaClass)
  @POST
  @Path("/login")
  @Produces(MediaType.APPLICATION_JSON)
  fun login(
      @QueryParam("email") email: String,
      @QueryParam("pwd") pwd: String): HmResponse {
    try {
      log.info("User login; email:$email.")
      val auth = UsersDao.login(email, pwd)
      return HmResponse.getOk(auth)
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
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
      return HmResponse.getFail(e.getCode())
    }

  }

  @POST
  @Path("/logout")
  @Produces(MediaType.APPLICATION_JSON)
  fun logout(
      @CookieParam("userId") userId: UUID,
      @CookieParam("authToken") authToken: UUID): HmResponse {
    UsersDao.logout(userId, authToken)
    log.info("User logout.")
    return HmResponse.getOk()
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  fun createBalanceSheet(bs: BalanceSheet) {
    MainDao.createBalanceSheet(bs.id!!)
  }

  @DELETE
  fun deleteBalanceSheet(@QueryParam("id") id: UUID) {
    MainDao.deleteBalanceSheet(id)
  }
}
