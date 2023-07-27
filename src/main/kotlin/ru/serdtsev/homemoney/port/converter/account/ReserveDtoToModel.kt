package ru.serdtsev.homemoney.port.converter.account

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.port.dto.account.ReserveDto
import ru.serdtsev.homemoney.domain.model.account.Reserve
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder

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