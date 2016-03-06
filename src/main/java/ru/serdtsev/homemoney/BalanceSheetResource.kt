package ru.serdtsev.homemoney

import ru.serdtsev.homemoney.dao.MainDao
import ru.serdtsev.homemoney.dto.HmResponse
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/{bsId}/bs-stat")
class BalanceSheetResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  fun getBalanceSheetInfo(
      @PathParam("bsId") bsId: UUID,
      @DefaultValue("30") @QueryParam("interval") interval: Long?): HmResponse {
    try {
      return HmResponse.getOk(MainDao.getBsStat(bsId, interval))
    } catch (e: HmException) {
      return HmResponse.getFail("INCORRECT_AUTH_TOKEN")
    }

  }
}
