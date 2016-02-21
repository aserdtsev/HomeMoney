package ru.serdtsev.homemoney;

import ru.serdtsev.homemoney.dao.MainDao;
import ru.serdtsev.homemoney.dto.BalanceSheet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("database")
public class DatabaseResource {
  @Path("/clear-n-create-balance-sheet")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public BalanceSheet clearDatabaseNCreateBalanceSheet() {
    MainDao.clearDatabase();
    UUID bsId = UUID.randomUUID();
    MainDao.createBalanceSheet(bsId);
    BalanceSheet bs = MainDao.getBalanceSheets().get(0);
    return bs;
  }
}
