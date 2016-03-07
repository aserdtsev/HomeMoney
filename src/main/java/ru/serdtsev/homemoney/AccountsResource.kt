package ru.serdtsev.homemoney

import ru.serdtsev.homemoney.dao.AccountsDao
import ru.serdtsev.homemoney.dto.HmResponse
import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/{bsId}/accounts")
class AccountsResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  fun getAccountList(@PathParam("bsId") bsId: UUID): HmResponse {
    val allAccounts = AccountsDao.getAccounts(bsId)
    return HmResponse.getOk(allAccounts)
  }
}
