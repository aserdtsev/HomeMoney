package ru.serdtsev.homemoney.account.converter

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.account.dto.ReserveDto
import ru.serdtsev.homemoney.account.model.Reserve
import ru.serdtsev.homemoney.common.ApiRequestContextHolder

class ReserveDtoToModel(private val appCtx: ApplicationContext) : Converter<ReserveDto, Reserve> {
    private val apiRequestContextHolder: ApiRequestContextHolder
        get() = appCtx.getBean(ApiRequestContextHolder::class.java)

    override fun convert(source: ReserveDto): Reserve {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        return with (source) {
            Reserve(id, balanceSheet, name, createdDate, currencyCode, value, target, isArc)
        }
    }
}