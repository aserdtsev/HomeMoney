package ru.serdtsev.homemoney.moneyoper.converter

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.account.model.Reserve
import ru.serdtsev.homemoney.moneyoper.dto.MoneyOperDto
import ru.serdtsev.homemoney.moneyoper.dto.MoneyOperItemDto
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperType
import ru.serdtsev.homemoney.moneyoper.model.Tag

class MoneyOperToDto(private val appCtx: ApplicationContext) : Converter<MoneyOper, MoneyOperDto> {
    private val conversionService: ConversionService
        get() = appCtx.getBean("conversionService") as ConversionService

    override fun convert(source: MoneyOper): MoneyOperDto =
        MoneyOperDto(source.id, source.status, source.performed, source.period, source.comment,
            getStringsByTags(source.tags), source.dateNum, source.getParentOperId(),
            source.recurrenceId, source.created).apply {
            type = if (source.type == MoneyOperType.transfer && source.items.any { it.balance is Reserve }) {
                val operItem = source.items.first { it.balance is Reserve }
                if (operItem.value.signum() > 0) MoneyOperType.income.name else MoneyOperType.expense.name
            } else
                source.type.name
            items = source.items
                .map { conversionService.convert(it, MoneyOperItemDto::class.java)!! }
                .sortedBy { it.value.multiply(it.sgn.toBigDecimal()) }
        }

    private fun getStringsByTags(tags: Collection<Tag>): List<String> = tags.map { it.name }
}