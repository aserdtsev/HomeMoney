package ru.serdtsev.homemoney.port.converter.moneyoper

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperItem
import ru.serdtsev.homemoney.domain.model.moneyoper.RecurrenceOper
import ru.serdtsev.homemoney.domain.repository.BalanceRepository
import ru.serdtsev.homemoney.domain.repository.RecurrenceOperRepository
import ru.serdtsev.homemoney.port.dto.moneyoper.RecurrenceOperDto
import ru.serdtsev.homemoney.port.service.MoneyOperService

class RecurrenceOperToModelConverter(private val applicationContext: ApplicationContext) : Converter<RecurrenceOperDto, RecurrenceOper> {
    private val recurrenceOperRepository: RecurrenceOperRepository
        get() = applicationContext.getBean(RecurrenceOperRepository::class.java)
    private val balanceRepository: BalanceRepository
        get() = applicationContext.getBean(BalanceRepository::class.java)
    private val moneyOperService: MoneyOperService
        get() = applicationContext.getBean(MoneyOperService::class.java)

    override fun convert(source: RecurrenceOperDto): RecurrenceOper {
        val recurrenceOper = recurrenceOperRepository.findById(source.id)
        val template = recurrenceOper.template
        template.apply {
            this.comment = source.comment
            this.items.clear()
            val items = source.items
                .map {
                    val balance = balanceRepository.findById(it.balanceId)
                    val value = it.value.multiply(it.sgn.toBigDecimal())
                    MoneyOperItem(it.id, template.id, balance, value, it.performedAt, it.index)
                }
                .toMutableList()
            this.items.addAll(items)

            this.tags.clear()
            val tags = moneyOperService.getTagsByStrings(source.tags)
            this.tags.addAll(tags)
        }
        return RecurrenceOper(source.id, template, source.nextDate)
    }
}