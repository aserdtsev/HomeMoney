package ru.serdtsev.homemoney

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.dao.CategoriesDao
import ru.serdtsev.homemoney.dto.Category
import ru.serdtsev.homemoney.dto.HmResponse
import java.util.*

@RestController
@RequestMapping("/api/{bsId}/categories")
class CategoriesResource {
  @RequestMapping
  fun getCategoryList(@PathVariable("bsId") bsId: UUID): HmResponse {
    return HmResponse.getOk(CategoriesDao.getCategories(bsId))
  }

  @RequestMapping("/create")
  fun createCategory(@PathVariable("bsId") bsId: UUID, category: Category): HmResponse =
    try {
      CategoriesDao.createCategory(bsId, category)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }

  @RequestMapping("/update")
  fun updateCategory(
      @PathVariable("bsId") bsId: String,
      category: Category): HmResponse =
    try {
      CategoriesDao.updateCategory(UUID.fromString(bsId), category)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }

  @RequestMapping("/delete")
  fun deleteCategory(
      @PathVariable("bsId") bsId: UUID,
      category: Category): HmResponse =
    try {
      CategoriesDao.deleteCategory(bsId, category.id!!)
      HmResponse.getOk()
    } catch (e: HmException) {
      HmResponse.getFail(e.getCode())
    }
}
