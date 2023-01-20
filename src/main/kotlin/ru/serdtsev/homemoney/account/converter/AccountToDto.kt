package ru.serdtsev.homemoney.account.converter

import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.account.dto.AccountDto
import ru.serdtsev.homemoney.account.model.Account

class AccountToDto : Converter<Account, AccountDto> {
    override fun convert(source: Account) =
        with(source) { AccountDto(id, type, name, createdDate, isArc) }
}