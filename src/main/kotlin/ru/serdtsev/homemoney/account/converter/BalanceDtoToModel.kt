package ru.serdtsev.homemoney.account.converter

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.account.dto.BalanceDto
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.common.ApiRequestContextHolder

class BalanceDtoToModel(private val appCtx: ApplicationContext) : Converter<BalanceDto, Balance> {
    val apiRequestContextHolder: ApiRequestContextHolder
        get() = appCtx.getBean(ApiRequestContextHolder::class.java)

    override fun convert(source: BalanceDto): Balance {
        return with (source) {
            val balanceSheet = apiRequestContextHolder.getBalanceSheet()
            Balance(id, balanceSheet, type, name, createdDate, isArc, value, currencyCode).also {
                it.minValue = minValue
                it.creditLimit = creditLimit
                it.num = num
            }
        }
    }
}