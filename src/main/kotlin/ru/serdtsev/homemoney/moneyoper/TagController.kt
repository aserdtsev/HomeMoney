package ru.serdtsev.homemoney.moneyoper

import org.springframework.core.convert.ConversionService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.web.bind.annotation.*
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.moneyoper.model.CategoryType
import ru.serdtsev.homemoney.moneyoper.model.Tag
import ru.serdtsev.homemoney.moneyoper.model.TagDto
import java.util.*

@RestController
@RequestMapping("/api/{bsId}/tags")
class TagController(
    val tagService: TagService,
    val conversionService: ConversionService,
    val balanceSheetRepo: BalanceSheetRepository
) {
    @RequestMapping
    fun getTags(
        @PathVariable bsId: UUID,
        search: String?,
        category: Boolean?,
        categoryType: CategoryType?
    ): HmResponse {
        val tags = tagService.getTags(bsId, search?.toLowerCase(), category, categoryType).map {
            conversionService.convert(it, TagDto::class.java)
        }
        return HmResponse.getOk(tags)
    }

    @RequestMapping(method = [RequestMethod.PUT])
    fun updateTag(@PathVariable bsId: UUID, @RequestBody tagDto: TagDto) {
        val balanceSheet = balanceSheetRepo.findByIdOrNull(bsId)!!
        val tag = Tag(tagDto.id, balanceSheet, tagDto.name, null, tagDto.isCategory, tagDto.categoryType, tagDto.isArc)
        tagService.updateTag(tag)
    }

    @RequestMapping("/{tagId}", method = [RequestMethod.DELETE])
    fun deleteTag(@PathVariable bsId: UUID, @PathVariable tagId: UUID) {

    }

}