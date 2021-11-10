package ru.serdtsev.homemoney.moneyoper.converter

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.dto.TagDto
import ru.serdtsev.homemoney.moneyoper.model.Tag

class TagDtoToModel(private val apiCtx: ApplicationContext) : Converter<TagDto, Tag> {
    private val apiRequestContextHolder: ApiRequestContextHolder
        get() = apiCtx.getBean(ApiRequestContextHolder::class.java)

    override fun convert(source: TagDto): Tag {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        return with (source) {
            Tag(id, balanceSheet, name, rootId, isCategory, categoryType, isArc)
        }
    }
}