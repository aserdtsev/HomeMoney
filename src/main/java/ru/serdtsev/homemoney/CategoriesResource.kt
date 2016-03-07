package ru.serdtsev.homemoney

import ru.serdtsev.homemoney.dao.CategoriesDao
import ru.serdtsev.homemoney.dto.Category
import ru.serdtsev.homemoney.dto.HmResponse
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/{bsId}/categories")
class CategoriesResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  fun getCategoryList(@PathParam("bsId") bsId: UUID): HmResponse {
    return HmResponse.getOk(CategoriesDao.getCategories(bsId))
  }

  @POST
  @Path("/create")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun createCategory(@PathParam("bsId") bsId: UUID, category: Category): HmResponse {
    try {
      CategoriesDao.createCategory(bsId, category)
      return HmResponse.ok
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }

  @POST
  @Path("/update")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun updateCategory(
      @PathParam("bsId") bsId: String,
      category: Category): HmResponse {
    try {
      CategoriesDao.updateCategory(UUID.fromString(bsId), category)
      return HmResponse.ok
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }

  @POST
  @Path("/delete")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun deleteCategory(
      @PathParam("bsId") bsId: UUID,
      category: Category): HmResponse {
    try {
      CategoriesDao.deleteCategory(bsId, category.id!!)
      return HmResponse.ok
    } catch (e: HmException) {
      return HmResponse.getFail(e.getCode())
    }

  }
}
