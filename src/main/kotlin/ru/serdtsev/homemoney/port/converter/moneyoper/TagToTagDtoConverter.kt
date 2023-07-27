package ru.serdtsev.homemoney.port.converter.moneyoper

import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.domain.model.moneyoper.Tag
import ru.serdtsev.homemoney.port.dto.moneyoper.TagDto

class TagToTagDtoConverter: Converter<Tag, TagDto> {
    override fun convert(source: Tag): TagDto = TagDto(source.id, source.name, source.rootId, source.isCategory,
        source.categoryType, source.arc)
}