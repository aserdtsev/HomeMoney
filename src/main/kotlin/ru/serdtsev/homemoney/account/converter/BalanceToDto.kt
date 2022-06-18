package ru.serdtsev.homemoney.account.converter

import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.account.dto.BalanceDto
import ru.serdtsev.homemoney.account.model.Balance

class BalanceToDto : Converter<Balance, BalanceDto> {
    override fun convert(source: Balance) =
        with(source) {
            BalanceDto(
                id, type, name, createdDate, isArc, value, currencyCode, currencySymbol, minValue!!,
                creditLimit!!, freeFunds, num!!
            )
        }
}