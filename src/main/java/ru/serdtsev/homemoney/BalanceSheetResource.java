package ru.serdtsev.homemoney;

import org.apache.log4j.NDC;
import ru.serdtsev.homemoney.dao.MainDao;
import ru.serdtsev.homemoney.dto.HmResponse;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import java.util.Stack;
import java.util.UUID;

@Path("/{bsId}/bs-stat")
public class BalanceSheetResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public void getBalanceSheetInfo(
      @PathParam("bsId") UUID bsId,
      @DefaultValue("30") @QueryParam("interval") Long interval,
      @Suspended AsyncResponse asyncResponse) {
    Stack logStack = NDC.cloneStack();
    HmExecutorService.getInstance().submit(() -> {
      NDC.inherit(logStack);
      HmResponse response;
      try {
        response = HmResponse.getOk(MainDao.INSTANCE.getBsStat(bsId, interval));
      } catch (HmException e) {
        response = HmResponse.getFail("INCORRECT_AUTH_TOKEN");
      }
      asyncResponse.resume(response);
      NDC.remove();
    });
  }
}
