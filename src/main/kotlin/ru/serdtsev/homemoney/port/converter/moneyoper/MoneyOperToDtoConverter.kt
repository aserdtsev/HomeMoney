package ru.serdtsev.homemoney.port.converter.moneyoper

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.port.dto.moneyoper.MoneyOperDto
import ru.serdtsev.homemoney.port.dto.moneyoper.MoneyOperItemDto
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.Tag

class MoneyOperToDtoConverter(private val appCtx: ApplicationContext) : Converter<MoneyOper, MoneyOperDto> {
    private val conversionService: ConversionService
        get() = appCtx.getBean("conversionService") as ConversionService

    override fun convert(source: MoneyOper): MoneyOperDto =
        MoneyOperDto(source.id, source.status, source.performed, source.period, source.comment,
            getStringsByTags(source.tags), source.dateNum, source.getParentOperId(),
            source.recurrenceId, source.created).apply {
            type = getOperType(source)
            items = source.items
                .map { conversionService.convert(it, MoneyOperItemDto::class.java)!! }
                .sortedBy { it.value.multiply(it.sgn.toBigDecimal()) }
        }

    private fun getStringsByTags(tags: Collection<Tag>): List<String> = tags.map { it.name }
}