package ru.serdtsev.homemoney;

import ru.serdtsev.homemoney.dao.ReservesDao;
import ru.serdtsev.homemoney.dto.Account;
import ru.serdtsev.homemoney.dto.HmResponse;
import ru.serdtsev.homemoney.dto.Reserve;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/{bsId}/reserves")
public class ReservesResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse getReserveList(@PathParam("bsId") UUID bsId) {
    return HmResponse.Companion.getOk(ReservesDao.INSTANCE.getReserves(bsId));
  }

  @POST
  @Path("/create")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse createReserve(
      @PathParam("bsId") UUID bsId,
      Reserve reserve) {
    try {
      reserve.setType(Account.Type.reserve);
      ReservesDao.INSTANCE.createReserve(bsId, reserve);
      return HmResponse.Companion.getOk();
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }

  @POST
  @Path("/update")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse updateReserve(
      @PathParam("bsId") UUID bsId,
      Reserve reserve) {
    try {
      ReservesDao.INSTANCE.updateReserve(bsId, reserve);
      return HmResponse.Companion.getOk();
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }

  @POST
  @Path("/delete")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse deleteReserve(
      @PathParam("bsId") UUID bsId,
      Reserve reserve) {
    try {
      ReservesDao.INSTANCE.deleteReserve(bsId, reserve.getId());
      return HmResponse.Companion.getOk();
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }
}
