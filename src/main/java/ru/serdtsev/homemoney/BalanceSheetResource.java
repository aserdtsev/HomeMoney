package ru.serdtsev.homemoney;

import ru.serdtsev.homemoney.dao.MainDao;
import ru.serdtsev.homemoney.dto.HmResponse;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/{bsId}/bs-stat")
public class BalanceSheetResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse getBalanceSheetInfo(
      @Suspended AsyncResponse asyncResponse,
      @PathParam("bsId") UUID bsId,
      @DefaultValue("30") @QueryParam("interval") Long interval) {
    try {
      return HmResponse.getOk(MainDao.INSTANCE.getBsStat(bsId, interval));
    } catch (HmException e) {
      return HmResponse.getFail("INCORRECT_AUTH_TOKEN");
    }
  }
}
