package ru.serdtsev.homemoney;

import com.google.common.base.Strings;
import ru.serdtsev.homemoney.dao.MoneyTrnTemplsDao;
import ru.serdtsev.homemoney.dto.HmResponse;
import ru.serdtsev.homemoney.dto.MoneyTrn;
import ru.serdtsev.homemoney.dto.MoneyTrnTempl;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Path("/{bsId}/money-trn-templs")
public class MoneyTrnTemplsResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse getList(
      @PathParam("bsId") UUID bsId,
      @QueryParam("search") String search) {
    try {
      List<MoneyTrnTempl> list = MoneyTrnTemplsDao.getMoneyTrnTempls(bsId, Optional.ofNullable(Strings.emptyToNull(search)));
      return HmResponse.Companion.getOk(list);
    } catch (HmException e) {
      return  HmResponse.Companion.getFail(e.getCode());
    }
  }

  @POST
  @Path("/create")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse create(
      @PathParam("bsId") UUID bsId,
      MoneyTrn moneyTrn) {
    MoneyTrnTempl templ = new MoneyTrnTempl(UUID.randomUUID(), moneyTrn.getId(), moneyTrn.getId(),
        MoneyTrnTempl.Companion.calcNextDate(moneyTrn.getTrnDate(), moneyTrn.getPeriod()), moneyTrn.getPeriod(),
        moneyTrn.getFromAccId(), moneyTrn.getToAccId(), moneyTrn.getAmount(),
        moneyTrn.getComment(), moneyTrn.getLabels());
    MoneyTrnTemplsDao.createMoneyTrnTempl(bsId, templ);
    return HmResponse.Companion.getOk();
  }

  @POST
  @Path("/skip")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse skip(
      @PathParam("bsId") UUID bsId,
      MoneyTrnTempl templ) {
    templ.setNextDate(MoneyTrnTempl.Companion.calcNextDate(templ.getNextDate(), templ.getPeriod()));
    MoneyTrnTemplsDao.updateMoneyTrnTempl(bsId, templ);
    return HmResponse.Companion.getOk();
  }

  @POST
  @Path("/delete")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse delete(
      @PathParam("bsId") UUID bsId,
      MoneyTrnTempl templ) {
    try {
      MoneyTrnTemplsDao.deleteMoneyTrnTempl(bsId, templ.getId());
      return HmResponse.Companion.getOk();
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }

  @POST
  @Path("/update")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse updateTempl(
      @PathParam("bsId") UUID bsId,
      MoneyTrnTempl templ) {
    try {
      MoneyTrnTemplsDao.updateMoneyTrnTempl(bsId, templ);
      return HmResponse.Companion.getOk();
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }

}
