package ru.serdtsev.homemoney.port.converter.account

import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.account.Credit
import ru.serdtsev.homemoney.port.dto.account.BalanceDto

class BalanceDtoToModel : Converter<BalanceDto, Balance> {
    override fun convert(source: BalanceDto): Balance {
        return with (source) {
            Balance(id, type, name, createdDate, isArc, currencyCode, value).also {
                it.minValue = minValue
                it.credit = creditLimit?.let { Credit(creditLimit, null) }
                it.num = num
            }
        }
    }
}