package ru.serdtsev.homemoney.moneyoper.converter

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.account.dao.BalanceDao
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.dto.MoneyOperDto
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperItem
import ru.serdtsev.homemoney.moneyoper.model.Period
import ru.serdtsev.homemoney.moneyoper.service.MoneyOperService

class MoneyOperDtoToModel(private val appCtx: ApplicationContext) : Converter<MoneyOperDto, MoneyOper> {
    private val apiRequestContextHolder: ApiRequestContextHolder
        get() = appCtx.getBean(ApiRequestContextHolder::class.java)
    private val balanceDao: BalanceDao
        get() = appCtx.getBean(BalanceDao::class.java)
    private val moneyOperService: MoneyOperService
        get() = appCtx.getBean(MoneyOperService::class.java)

    override fun convert(source: MoneyOperDto): MoneyOper {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        val dateNum = source.dateNum ?: 0
        val tags = moneyOperService.getTagsByStrings(balanceSheet, source.tags)
        val period = source.period ?: Period.month
        val items = source.items
            .map {
                val balance = balanceDao.findById(it.balanceId)
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