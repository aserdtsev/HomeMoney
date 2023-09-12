package ru.serdtsev.homemoney.port.converter.moneyoper

import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.port.dto.moneyoper.TagDto
import ru.serdtsev.homemoney.domain.model.moneyoper.Tag

class TagDtoToModelConverter : Converter<TagDto, Tag> {
    override fun convert(source: TagDto): Tag {
        return with (source) {
            Tag(id, name, categoryType, rootId, isArc ?: false)
        }
    }
}