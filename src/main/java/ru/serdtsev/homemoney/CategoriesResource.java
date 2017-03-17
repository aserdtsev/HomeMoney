package ru.serdtsev.homemoney;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.dao.CategoriesDao;
import ru.serdtsev.homemoney.dto.Category;
import ru.serdtsev.homemoney.dto.HmResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/{bsId}/categories")
public class CategoriesResource {
  private CategoriesDao categoriesDao;

  @Autowired
  public CategoriesResource(CategoriesDao categoriesDao) {
    this.categoriesDao = categoriesDao;
  }

  @RequestMapping
  public HmResponse getCategoryList(@PathVariable UUID bsId) {
    return HmResponse.getOk(CategoriesDao.getCategories(bsId));
  }

  @RequestMapping({"/create"})
  public HmResponse createCategory(
      @PathVariable UUID bsId,
      @RequestBody Category category) {
    try {
      CategoriesDao.createCategory(bsId, category);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping({"/update"})
  public HmResponse updateCategory(
      @PathVariable UUID bsId,
      @RequestBody Category category) {
    try {
      CategoriesDao.updateCategory(bsId, category);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping({"/delete"})
  public HmResponse deleteCategory(
      @PathVariable UUID bsId,
      @RequestBody Category category) {
    try {
      categoriesDao.deleteCategory(bsId, category.getId());
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }
}
