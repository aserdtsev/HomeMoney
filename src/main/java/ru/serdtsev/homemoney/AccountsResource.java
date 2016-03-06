package ru.serdtsev.homemoney;

import ru.serdtsev.homemoney.dao.AccountsDao;
import ru.serdtsev.homemoney.dto.Account;
import ru.serdtsev.homemoney.dto.HmResponse;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

@Path("/{bsId}/accounts")
public class AccountsResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse getAccountList(@PathParam("bsId") UUID bsId) {
    List<Account> allAccounts = AccountsDao.INSTANCE.getAccounts(bsId);
    return HmResponse.Companion.getOk(allAccounts);
  }
}
