package ru.serdtsev.homemoney

import ru.serdtsev.homemoney.dao.MoneyTrnTemplsDao
import ru.serdtsev.homemoney.dto.HmResponse
import ru.serdtsev.homemoney.dto.MoneyTrn
import ru.serdtsev.homemoney.dto.MoneyTrnTempl
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/{bsId}/money-trn-templs")
class MoneyTrnTemplsResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  fun getList(
      @PathParam("bsId") bsId: UUID,
      @QueryParam("search") search: String?): HmResponse {
    try {
      val list = MoneyTrnTemplsDao.getMoneyTrnTempls(bsId, search)
      return HmResponse.getOk(list)
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }

  @POST
  @Path("/create")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun create(
      @PathParam("bsId") bsId: UUID,
      moneyTrn: MoneyTrn): HmResponse {
    val nextDate = MoneyTrnTempl.calcNextDate(moneyTrn.trnDate!!, moneyTrn.period!!)
    val templ = MoneyTrnTempl(UUID.randomUUID(), moneyTrn.id!!, moneyTrn.id!!, nextDate,
        moneyTrn.period!!, moneyTrn.fromAccId!!, moneyTrn.toAccId!!, moneyTrn.amount!!,
        moneyTrn.comment, moneyTrn.labels)
    MoneyTrnTemplsDao.createMoneyTrnTempl(bsId, templ)
    return HmResponse.getOk()
  }

  @POST
  @Path("/skip")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun skip(
      @PathParam("bsId") bsId: UUID,
      templ: MoneyTrnTempl): HmResponse {
    templ.nextDate = MoneyTrnTempl.calcNextDate(templ.nextDate!!, templ.period!!)
    MoneyTrnTemplsDao.updateMoneyTrnTempl(bsId, templ)
    return HmResponse.getOk()
  }

  @POST
  @Path("/delete")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun delete(
      @PathParam("bsId") bsId: UUID,
      templ: MoneyTrnTempl): HmResponse =
    try {
      MoneyTrnTemplsDao.deleteMoneyTrnTempl(bsId, templ.id!!)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }

  @POST
  @Path("/update")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun updateTempl(
      @PathParam("bsId") bsId: UUID,
      templ: MoneyTrnTempl): HmResponse =
    try {
      MoneyTrnTemplsDao.updateMoneyTrnTempl(bsId, templ)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }

}
