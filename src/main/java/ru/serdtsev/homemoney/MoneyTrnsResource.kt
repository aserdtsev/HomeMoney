package ru.serdtsev.homemoney

import ru.serdtsev.homemoney.dao.MoneyTrnsDao
import ru.serdtsev.homemoney.dto.HmResponse
import ru.serdtsev.homemoney.dto.MoneyTrn
import ru.serdtsev.homemoney.dto.PagedList
import java.sql.Date
import java.time.LocalDate
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/{bsId}/money-trns")
class MoneyTrnsResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  fun getMoneyTrns(
      @PathParam("bsId") bsId: UUID,
      @QueryParam("search") search: String?,
      @DefaultValue("10") @QueryParam("limit") limit: Int,
      @DefaultValue("0") @QueryParam("offset") offset: Int): HmResponse {
    try {
      val trns = ArrayList<MoneyTrn>()

      if (offset == 0) {
        val beforeDate = LocalDate.now().plusDays(14)
        val pendingTrns = MoneyTrnsDao.getPendingMoneyTrns(bsId, search, Date.valueOf(beforeDate))
        trns.addAll(pendingTrns)
      }

      // Запросим на одну операцию больше, чем нужно, чтобы понять, есть ли еще (hasNext).
      val doneTrns = MoneyTrnsDao.getDoneMoneyTrns(bsId, search, limit + 1, offset)
      val hasNext = doneTrns.size > limit
      trns.addAll(if (hasNext) doneTrns.subList(0, limit) else doneTrns)

      return HmResponse.getOk(PagedList(trns, limit, offset, hasNext))
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }
  }

  @GET
  @Path("item")
  @Produces(MediaType.APPLICATION_JSON)
  fun getMoneyTrn(
      @PathParam("bsId") bsId: UUID,
      @QueryParam("id") id: UUID): HmResponse {
    try {
      val moneyTrn = MoneyTrnsDao.getMoneyTrn(bsId, id)
      return HmResponse.getOk(moneyTrn)
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }

  @POST
  @Path("/create")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun createMoneyTrn(
      @PathParam("bsId") bsId: UUID,
      moneyTrn: MoneyTrn): HmResponse {
    return HmResponse.getOk(MoneyTrnsDao.createMoneyTrn(bsId, moneyTrn))
  }

  @POST
  @Path("/delete")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun deleteMoneyTrn(
      @PathParam("bsId") bsId: UUID,
      moneyTrn: MoneyTrn): HmResponse {
    try {
      MoneyTrnsDao.deleteMoneyTrn(bsId, moneyTrn.id!!)
      return HmResponse.ok
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }

  @POST
  @Path("/update")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun updateMoneyTrn(
      @PathParam("bsId") bsId: UUID,
      moneyTrn: MoneyTrn): HmResponse {
    try {
      MoneyTrnsDao.updateMoneyTrn(bsId, moneyTrn)
      return HmResponse.ok
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }

  @POST
  @Path("/skip")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun skipMoneyTrn(
      @PathParam("bsId") bsId: UUID,
      moneyTrn: MoneyTrn): HmResponse {
    try {
      MoneyTrnsDao.skipMoneyTrn(bsId, moneyTrn)
      return HmResponse.ok
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }

  @POST
  @Path("/up")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun upMoneyTrn(
      @PathParam("bsId") bsId: UUID,
      moneyTrn: MoneyTrn): HmResponse {
    try {
      MoneyTrnsDao.upMoneyTrn(bsId, moneyTrn.id!!)
      return HmResponse.ok
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }
}
