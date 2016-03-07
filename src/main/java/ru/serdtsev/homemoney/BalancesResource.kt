package ru.serdtsev.homemoney

import ru.serdtsev.homemoney.dao.BalancesDao
import ru.serdtsev.homemoney.dto.Balance
import ru.serdtsev.homemoney.dto.HmResponse
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/{bsId}/balances")
class BalancesResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  fun getBalances(@PathParam("bsId") bsId: UUID): HmResponse {
    return HmResponse.getOk(BalancesDao.getBalances(bsId))
  }

  @POST
  @Path("/create")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun createBalance(
      @PathParam("bsId") bsId: UUID,
      balance: Balance): HmResponse {
    try {
      BalancesDao.createBalance(bsId, balance)
      return HmResponse.ok
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }

  @POST
  @Path("/update")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun updateBalance(
      @PathParam("bsId") bsId: UUID,
      balance: Balance): HmResponse {
    try {
      BalancesDao.updateBalance(bsId, balance)
      return HmResponse.ok
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }

  @POST
  @Path("/delete")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun deleteBalance(
      @PathParam("bsId") bsId: UUID,
      balance: Balance): HmResponse {
    try {
      BalancesDao.deleteBalance(bsId, balance.id!!)
      return HmResponse.ok
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }

  @POST
  @Path("/up")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun upBalance(
      @PathParam("bsId") bsId: UUID,
      balance: Balance): HmResponse {
    try {
      BalancesDao.upBalance(bsId, balance)
      return HmResponse.ok
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }
}
