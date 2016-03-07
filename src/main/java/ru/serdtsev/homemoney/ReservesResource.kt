package ru.serdtsev.homemoney

import ru.serdtsev.homemoney.dao.ReservesDao
import ru.serdtsev.homemoney.dto.Account
import ru.serdtsev.homemoney.dto.HmResponse
import ru.serdtsev.homemoney.dto.Reserve
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/{bsId}/reserves")
class ReservesResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  fun getReserveList(@PathParam("bsId") bsId: UUID): HmResponse {
    return HmResponse.getOk(ReservesDao.getReserves(bsId))
  }

  @POST
  @Path("/create")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun createReserve(
      @PathParam("bsId") bsId: UUID,
      reserve: Reserve): HmResponse {
    try {
      reserve.type = Account.Type.reserve
      ReservesDao.createReserve(bsId, reserve)
      return HmResponse.ok
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }

  @POST
  @Path("/update")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun updateReserve(
      @PathParam("bsId") bsId: UUID,
      reserve: Reserve): HmResponse {
    try {
      ReservesDao.updateReserve(bsId, reserve)
      return HmResponse.ok
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }

  @POST
  @Path("/delete")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun deleteReserve(
      @PathParam("bsId") bsId: UUID,
      reserve: Reserve): HmResponse {
    try {
      ReservesDao.deleteReserve(bsId, reserve.id!!)
      return HmResponse.ok
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }
}
