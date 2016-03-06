package ru.serdtsev.homemoney;

import ru.serdtsev.homemoney.dao.CategoriesDao;
import ru.serdtsev.homemoney.dto.Category;
import ru.serdtsev.homemoney.dto.HmResponse;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/{bsId}/categories")
public class CategoriesResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse getCategoryList(@PathParam("bsId") UUID bsId) {
    return HmResponse.Companion.getOk(CategoriesDao.INSTANCE.getCategories(bsId));
  }

  @POST
  @Path("/create")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse createCategory(@PathParam("bsId") UUID bsId, Category category) {
    try {
      CategoriesDao.INSTANCE.createCategory(bsId, category);
      return HmResponse.Companion.getOk();
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }

  @POST
  @Path("/update")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse updateCategory(
      @PathParam("bsId") String bsId,
      Category category
  ) {
    try {
      CategoriesDao.INSTANCE.updateCategory(UUID.fromString(bsId), category);
      return HmResponse.Companion.getOk();
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }

  @POST
  @Path("/delete")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse deleteCategory(
      @PathParam("bsId") UUID bsId,
      Category category) {
    try {
      CategoriesDao.INSTANCE.deleteCategory(bsId, category.getId());
      return HmResponse.Companion.getOk();
    } catch (HmException e) {
      return HmResponse.Companion.getFail(e.getCode());
    }
  }
}
