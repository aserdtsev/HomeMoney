package ru.serdtsev.homemoney.port.converter.moneyoper

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperItem
import ru.serdtsev.homemoney.domain.model.moneyoper.Period
import ru.serdtsev.homemoney.domain.repository.BalanceRepository
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import ru.serdtsev.homemoney.port.dto.moneyoper.MoneyOperDto
import ru.serdtsev.homemoney.port.service.MoneyOperService

class MoneyOperDtoToModelConverter(private val appCtx: ApplicationContext) : Converter<MoneyOperDto, MoneyOper> {
    private val apiRequestContextHolder: ApiRequestContextHolder
        get() = appCtx.getBean(ApiRequestContextHolder::class.java)
    private val balanceRepository: BalanceRepository
        get() = appCtx.getBean(BalanceRepository::class.java)
    private val moneyOperService: MoneyOperService
        get() = appCtx.getBean(MoneyOperService::class.java)

    override fun convert(source: MoneyOperDto): MoneyOper {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        val dateNum = source.dateNum
        val tags = moneyOperService.getTagsByStrings(balanceSheet, source.tags)
        val period = source.period ?: Period.month
        val items = source.items
            .map {
                val balance = balanceRepository.findById(it.balanceId)
                val value = it.value.multiply(it.sgn.toBigDecimal())
                MoneyOperItem(it.id, source.id, balance, value, it.performedAt, it.index)
            }
            .toMutableList()
        return MoneyOper(source.id, balanceSheet, items, source.status, source.operDate, dateNum, tags,
            source.comment, period).apply {
               this.recurrenceId = source.recurrenceId
        }
    }
}