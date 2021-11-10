package ru.serdtsev.homemoney.moneyoper

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.web.bind.annotation.*
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.moneyoper.dto.TagDto
import ru.serdtsev.homemoney.moneyoper.model.CategoryType
import ru.serdtsev.homemoney.moneyoper.model.Tag
import ru.serdtsev.homemoney.moneyoper.service.TagService
import java.util.*

@RestController
@RequestMapping("/api/tags")
class TagController(
    private val tagService: TagService,
    @Qualifier("conversionService") private val conversionService: ConversionService
) {
    @GetMapping
    fun getTags(search: String?, category: Boolean?, categoryType: CategoryType?): HmResponse {
        val tags = tagService.getTags(search?.lowercase(), category, categoryType)
            .map { conversionService.convert(it, TagDto::class.java) }
        return HmResponse.getOk(tags)
    }

    @PutMapping
    fun updateTag(@RequestBody tagDto: TagDto) {
        val tag = conversionService.convert(tagDto, Tag::class.java)!!
        tagService.update(tag)
    }

    @DeleteMapping("/{tagId}")
    fun deleteTag(@PathVariable tagId: UUID) {

    }

}