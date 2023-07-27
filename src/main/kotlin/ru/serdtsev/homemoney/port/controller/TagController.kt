package ru.serdtsev.homemoney.port.controller

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.web.bind.annotation.*
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.port.common.HmResponse
import ru.serdtsev.homemoney.port.dto.moneyoper.TagDto
import ru.serdtsev.homemoney.domain.model.moneyoper.CategoryType
import ru.serdtsev.homemoney.domain.model.moneyoper.Tag
import ru.serdtsev.homemoney.port.service.TagService
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
        DomainEventPublisher.instance.publish(tag)
    }

    @DeleteMapping("/{tagId}")
    fun deleteTag(@PathVariable tagId: UUID) {

    }

}