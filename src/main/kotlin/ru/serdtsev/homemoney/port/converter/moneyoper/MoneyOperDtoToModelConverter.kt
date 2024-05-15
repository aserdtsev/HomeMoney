package ru.serdtsev.homemoney.port.converter.moneyoper

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.domain.repository.BalanceRepository
import ru.serdtsev.homemoney.port.dto.moneyoper.DayRecurrenceParamsDto
import ru.serdtsev.homemoney.port.dto.moneyoper.MoneyOperDto
import ru.serdtsev.homemoney.port.dto.moneyoper.RecurrenceParamsDto
import ru.serdtsev.homemoney.port.service.TagService

class MoneyOperDtoToModelConverter(private val applicationContext: ApplicationContext) : Converter<MoneyOperDto, MoneyOper> {
    private val balanceRepository: BalanceRepository
        get() = applicationContext.getBean(BalanceRepository::class.java)
    private val tagService: TagService
        get() = applicationContext.getBean(TagService::class.java)

    override fun convert(source: MoneyOperDto): MoneyOper {
        val dateNum = source.dateNum
        val tags = tagService.getTagsByStrings(source.tags)
        val period = source.period ?: Period.Day
        val items = source.items
            .map {
                val balance = balanceRepository.findById(it.balanceId)
                val value = it.value.multiply(it.sgn.toBigDecimal())
                MoneyOperItem.of(source.id, balance, value, it.performedAt, it.index, id = it.id)
            }
            .toMutableList()
        return MoneyOper(source.id, items, source.status, source.operDate, tags, source.comment,
            period, toRecurrenceParams(source.recurrenceParams), source.recurrenceId, dateNum)
    }

    private fun toRecurrenceParams(source: RecurrenceParamsDto?): RecurrenceParams? {
        return when (source?.data) {
            is DayRecurrenceParamsDto -> DayRecurrenceParams((source.data as DayRecurrenceParamsDto).n)
            else -> null
        }
    }
}