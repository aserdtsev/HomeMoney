package ru.serdtsev.homemoney;

import ru.serdtsev.homemoney.dao.BalancesDao;
import ru.serdtsev.homemoney.dto.HmResponse;
import ru.serdtsev.homemoney.dto.Balance;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/{bsId}/balances")
public class BalancesResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse getBalances(@PathParam("bsId") UUID bsId) {
    return HmResponse.Companion.getOk(BalancesDao.INSTANCE.getBalances(bsId));
  }

  @POST
  @Path("/create")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse createBalance(
      @PathParam("bsId") UUID bsId,
      Balance balance) {
    try {
      BalancesDao.INSTANCE.createBalance(bsId, balance);
      return HmResponse.Companion.getOk();
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }

  @POST
  @Path("/update")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse updateBalance(
      @PathParam("bsId") UUID bsId,
      Balance balance) {
    try {
      BalancesDao.INSTANCE.updateBalance(bsId, balance);
      return HmResponse.Companion.getOk();
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }

  @POST
  @Path("/delete")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse deleteBalance(
      @PathParam("bsId") UUID bsId,
      Balance balance) {
    try {
      BalancesDao.INSTANCE.deleteBalance(bsId, balance.getId());
      return HmResponse.Companion.getOk();
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }

  @POST
  @Path("/up")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse upBalance(
      @PathParam("bsId") UUID bsId,
      Balance balance) {
    try {
      BalancesDao.INSTANCE.upBalance(bsId, balance);
      return HmResponse.Companion.getOk();
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }
}
