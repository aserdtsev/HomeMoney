package ru.serdtsev.homemoney.port.converter.moneyoper

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import ru.serdtsev.homemoney.port.dto.moneyoper.TagDto
import ru.serdtsev.homemoney.domain.model.moneyoper.Tag

class TagDtoToModelConverter(private val apiCtx: ApplicationContext) : Converter<TagDto, Tag> {
    private val apiRequestContextHolder: ApiRequestContextHolder
        get() = apiCtx.getBean(ApiRequestContextHolder::class.java)

    override fun convert(source: TagDto): Tag {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        return with (source) {
            Tag(id, balanceSheet, name, rootId, isCategory ?: false, categoryType, isArc ?: false)
        }
    }
}