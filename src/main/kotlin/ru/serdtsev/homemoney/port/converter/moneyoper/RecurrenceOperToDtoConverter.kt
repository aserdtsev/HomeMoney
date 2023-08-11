package ru.serdtsev.homemoney.port.converter.moneyoper

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.domain.model.moneyoper.RecurrenceOper
import ru.serdtsev.homemoney.domain.model.moneyoper.Tag
import ru.serdtsev.homemoney.port.dto.moneyoper.MoneyOperItemDto
import ru.serdtsev.homemoney.port.dto.moneyoper.RecurrenceOperDto

class RecurrenceOperToDtoConverter(private val appapplicationContexttx: ApplicationContext) : Converter<RecurrenceOper, RecurrenceOperDto> {
    private val conversionService: ConversionService
        get() = appapplicationContexttx.getBean("conversionService") as ConversionService

    override fun convert(source: RecurrenceOper): RecurrenceOperDto {
        val template = source.template
        val type = getOperType(template)
        val target = RecurrenceOperDto(source.id, source.nextDate, template.period!!, template.comment,
            getStringsByTags(template.tags), type)
        val items = template.items
            .map { conversionService.convert(it, MoneyOperItemDto::class.java)!! }
            .sortedBy { it.value.multiply(it.sgn.toBigDecimal()) }
        target.items = items
        return target
    }

    private fun getStringsByTags(tags: Collection<Tag>): List<String> = tags.map(Tag::name)
}