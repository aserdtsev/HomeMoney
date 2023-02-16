package ru.serdtsev.homemoney.moneyoper.converter

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.moneyoper.dto.MoneyOperItemDto
import ru.serdtsev.homemoney.moneyoper.dto.RecurrenceOperDto
import ru.serdtsev.homemoney.moneyoper.model.RecurrenceOper
import ru.serdtsev.homemoney.moneyoper.model.Tag

class RecurrenceOperToDto(private val appCtx: ApplicationContext) : Converter<RecurrenceOper, RecurrenceOperDto> {
    private val conversionService: ConversionService
        get() = appCtx.getBean("conversionService") as ConversionService

    override fun convert(source: RecurrenceOper): RecurrenceOperDto? {
        val oper = source.template
        val type = getOperType(oper)
        val target = RecurrenceOperDto(source.id, oper.id, oper.id,
            source.nextDate, oper.period!!, oper.comment, getStringsByTags(oper.tags), type)
        val items = oper.items
            .map { conversionService.convert(it, MoneyOperItemDto::class.java)!! }
            .sortedBy { it.value.multiply(it.sgn.toBigDecimal()) }
        target.items = items
        return target
    }

    private fun getStringsByTags(tags: Collection<Tag>): List<String> = tags.map(Tag::name)
}