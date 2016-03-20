package ru.serdtsev.homemoney

import ru.serdtsev.homemoney.dao.ReferencesDao
import ru.serdtsev.homemoney.dto.HmResponse
import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/{bsId}/references")
class ReferencesResource {
  @Path("currencies")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  fun getCurrencies(@PathParam("bsId") bsId: UUID): HmResponse = HmResponse.getOk(ReferencesDao.getCurrencies(bsId))
}