package ru.serdtsev.homemoney.port.converter.account

import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.port.dto.account.BalanceDto
import ru.serdtsev.homemoney.domain.model.account.Balance

class BalanceToDto : Converter<Balance, BalanceDto> {
    override fun convert(source: Balance) =
        with(source) {
            BalanceDto(
                id, type, name, createdDate, isArc, value, currencyCode, currencySymbol, minValue,
                credit?.creditLimit, freeFunds, num!!
            )
        }
}