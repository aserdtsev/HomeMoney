package ru.serdtsev.homemoney;

import ru.serdtsev.homemoney.dao.MoneyTrnsDao;
import ru.serdtsev.homemoney.dto.HmResponse;
import ru.serdtsev.homemoney.dto.MoneyTrn;
import ru.serdtsev.homemoney.dto.PagedList;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/{bsId}/money-trns")
public class MoneyTrnsResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse getMoneyTrns(
      @PathParam("bsId") UUID bsId,
      @QueryParam("search") String search,
      @DefaultValue("10") @QueryParam("limit") int limit,
      @DefaultValue("0") @QueryParam("offset") int offset)
  {
    try {
      List<MoneyTrn> trns = new ArrayList();

      if (offset == 0) {
        LocalDate beforeDate = LocalDate.now().plusDays(14);
        List<MoneyTrn> pendingTrns = MoneyTrnsDao.getPendingMoneyTrns(bsId, search, Date.valueOf(beforeDate));
        trns.addAll(pendingTrns);
      }

      // Запросим на одну операцию больше, чем нужно, чтобы понять, есть ли еще (hasNext).
      List<MoneyTrn> doneTrns = MoneyTrnsDao.getDoneMoneyTrns(bsId, search, limit+1, offset);
      Boolean hasNext = doneTrns.size() > limit;
      trns.addAll(hasNext ? doneTrns.subList(0, limit) : doneTrns);

      PagedList<MoneyTrn> pagedList = new PagedList<>(trns, limit, offset, hasNext);
      return HmResponse.Companion.getOk(pagedList);
    } catch (HmException e) {
      return  HmResponse.Companion.getFail(e.getCode());
    }
  }

  @GET
  @Path("item")
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse getMoneyTrn(
      @PathParam("bsId") UUID bsId,
      @QueryParam("id") UUID id)
  {
    try {
      MoneyTrn moneyTrn = MoneyTrnsDao.getMoneyTrn(bsId, id);
      return HmResponse.Companion.getOk(moneyTrn);
    } catch (HmException e) {
      return  HmResponse.Companion.getFail(e.getCode());
    }
  }

  @POST
  @Path("/create")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse createMoneyTrn(
      @PathParam("bsId") UUID bsId,
      MoneyTrn moneyTrn) {
    return HmResponse.Companion.getOk(MoneyTrnsDao.createMoneyTrn(bsId, moneyTrn));
  }

  @POST
  @Path("/delete")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse deleteMoneyTrn(
      @PathParam("bsId") UUID bsId,
      MoneyTrn moneyTrn) {
    try {
      MoneyTrnsDao.deleteMoneyTrn(bsId, moneyTrn.getId());
      return HmResponse.Companion.getOk();
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }

  @POST
  @Path("/update")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse updateMoneyTrn(
      @PathParam("bsId") UUID bsId,
      MoneyTrn moneyTrn) {
    try {
      MoneyTrnsDao.updateMoneyTrn(bsId, moneyTrn);
      return HmResponse.Companion.getOk();
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }

  @POST
  @Path("/skip")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse skipMoneyTrn(
      @PathParam("bsId") UUID bsId,
      MoneyTrn moneyTrn) {
    try {
      MoneyTrnsDao.skipMoneyTrn(bsId, moneyTrn);
      return HmResponse.Companion.getOk();
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }

  @POST
  @Path("/up")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse upMoneyTrn(
      @PathParam("bsId") UUID bsId,
      MoneyTrn moneyTrn) {
    try {
      MoneyTrnsDao.upMoneyTrn(bsId, moneyTrn.getId());
      return HmResponse.Companion.getOk();
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }
}
