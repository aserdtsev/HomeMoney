package ru.serdtsev.homemoney.moneyoper.converter

import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.moneyoper.model.Tag
import ru.serdtsev.homemoney.moneyoper.dto.TagDto

class TagToTagDto: Converter<Tag, TagDto> {
    override fun convert(source: Tag): TagDto = TagDto(source.id, source.name, source.rootId, source.isCategory,
        source.categoryType, source.arc)
}