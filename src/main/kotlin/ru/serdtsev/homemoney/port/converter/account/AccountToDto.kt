package ru.serdtsev.homemoney.port.converter.account

import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.port.dto.account.AccountDto
import ru.serdtsev.homemoney.domain.model.account.Account

class AccountToDto : Converter<Account, AccountDto> {
    override fun convert(source: Account) =
        with(source) { AccountDto(id, type, name, createdDate, isArc) }
}