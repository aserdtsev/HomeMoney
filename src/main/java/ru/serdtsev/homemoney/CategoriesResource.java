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
    return HmResponse.getOk(CategoriesDao.getCategories(bsId));
  }

  @POST
  @Path("/create")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public HmResponse createCategory(@PathParam("bsId") UUID bsId, Category category) {
    try {
      CategoriesDao.createCategory(bsId, category);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
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
      CategoriesDao.updateCategory(UUID.fromString(bsId), category);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
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
      CategoriesDao.deleteCategory(bsId, category.getId());
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }
}
