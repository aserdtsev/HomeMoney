package ru.serdtsev.homemoney

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.account.CategoryRepository
import ru.serdtsev.homemoney.account.model.Category
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository
import ru.serdtsev.homemoney.common.HmException
import ru.serdtsev.homemoney.common.HmResponse
import java.util.*

@RestController
@RequestMapping("/api/{bsId}/categories")
class CategoriesResource @Autowired constructor(
        private val balanceSheetRepo: BalanceSheetRepository,
        private val categoryRepo: CategoryRepository
) {
    @RequestMapping
    @Transactional(readOnly = true)
    fun getCategoryList(@PathVariable bsId: UUID): HmResponse {
        val balanceSheet = balanceSheetRepo.findByIdOrNull(bsId)!!
        val categories: List<Category> = categoryRepo.findByBalanceSheet(balanceSheet).sortedBy { it.sortIndex }
        return HmResponse.getOk(categories)
    }

    @RequestMapping("/create")
    fun createCategory(
            @PathVariable bsId: UUID,
            @RequestBody category: Category
    ): HmResponse {
        return try {
            val balanceSheet = balanceSheetRepo.findByIdOrNull(bsId)!!
            category.balanceSheet = balanceSheet
            categoryRepo.save(category)
            HmResponse.getOk()
        } catch (e: HmException) {
            HmResponse.getFail(e.code)
        }
    }

    @RequestMapping("/update")
    fun updateCategory(
            @PathVariable bsId: UUID?,
            @RequestBody category: Category
    ): HmResponse {
        return try {
            category.init(categoryRepo)
            categoryRepo.save(category)
            HmResponse.getOk()
        } catch (e: HmException) {
            HmResponse.getFail(e.code)
        }
    }

}