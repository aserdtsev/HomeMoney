package ru.serdtsev.homemoney.moneyoper

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.web.bind.annotation.*
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.moneyoper.model.CategoryType
import ru.serdtsev.homemoney.moneyoper.model.Tag
import ru.serdtsev.homemoney.moneyoper.dto.TagDto
import java.util.*

@RestController
@RequestMapping("/api/tags")
class TagController(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val tagService: TagService,
    @Qualifier("conversionService") private val conversionService: ConversionService
) {
    @RequestMapping
    fun getTags(search: String?, category: Boolean?, categoryType: CategoryType?): HmResponse {
        val bsId = apiRequestContextHolder.getBsId()
        val tags = tagService.getTags(bsId, search?.lowercase(Locale.getDefault()), category, categoryType).map {
            conversionService.convert(it, TagDto::class.java)
        }
        return HmResponse.getOk(tags)
    }

    @RequestMapping(method = [RequestMethod.PUT])
    fun updateTag(@RequestBody tagDto: TagDto) {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        val tag = Tag(tagDto.id, balanceSheet, tagDto.name, null, tagDto.isCategory, tagDto.categoryType, tagDto.isArc)
        tagService.updateTag(tag)
    }

    @RequestMapping("/{tagId}", method = [RequestMethod.DELETE])
    fun deleteTag(@PathVariable tagId: UUID) {

    }

}