package ru.serdtsev.homemoney.port.converter.account

import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.port.dto.account.ReserveDto
import ru.serdtsev.homemoney.domain.model.account.Reserve
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

class ReserveDtoToModel(val clock: Clock) : Converter<ReserveDto, Reserve> {
    override fun convert(source: ReserveDto): Reserve = with (source) {
        val createdDate = source.createdDate ?: LocalDate.now(clock)
        val currencyCode = source.currencyCode ?: ApiRequestContextHolder.balanceSheet.currencyCode
        val value = source.value ?: BigDecimal.ZERO
        val target = source.target ?: BigDecimal.ZERO
        val isArc = source.isArc ?: false
        Reserve(id, name, createdDate, currencyCode, value, target, isArc)
    }
}