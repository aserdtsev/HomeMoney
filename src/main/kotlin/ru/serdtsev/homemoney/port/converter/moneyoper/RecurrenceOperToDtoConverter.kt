package ru.serdtsev.homemoney.port.converter.moneyoper

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.domain.model.moneyoper.RecurrenceOper
import ru.serdtsev.homemoney.domain.model.moneyoper.Tag
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.port.dto.moneyoper.MoneyOperItemDto
import ru.serdtsev.homemoney.port.dto.moneyoper.RecurrenceOperDto

class RecurrenceOperToDtoConverter(private val appCtx: ApplicationContext) : Converter<RecurrenceOper, RecurrenceOperDto> {
    private val conversionService: ConversionService
        get() = appCtx.getBean("conversionService") as ConversionService

    override fun convert(source: RecurrenceOper): RecurrenceOperDto {
        val moneyOperRepository = appCtx.getBean(MoneyOperRepository::class.java)
        val oper = moneyOperRepository.findById(source.templateId)
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