package ru.serdtsev.homemoney

import ru.serdtsev.homemoney.dao.MainDao
import ru.serdtsev.homemoney.dto.BalanceSheet
import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("database")
class DatabaseResource {
  @Path("/clear-n-create-balance-sheet")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  fun clearDatabaseNCreateBalanceSheet(): BalanceSheet {
    MainDao.clearDatabase()
    MainDao.createBalanceSheet(UUID.randomUUID())
    return MainDao.balanceSheets[0]
  }
}
