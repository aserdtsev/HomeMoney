package ru.serdtsev.homemoney.moneyoper.converter

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.account.BalanceRepository
import ru.serdtsev.homemoney.moneyoper.dto.MoneyOperItemDto
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperItem

class MoneyOperItemToDto(private val appCtx: ApplicationContext) : Converter<MoneyOperItem, MoneyOperItemDto> {
    private val balanceRepo: BalanceRepository
        get() = appCtx.getBean(BalanceRepository::class.java)

    override fun convert(item: MoneyOperItem): MoneyOperItemDto {
        val balance = balanceRepo.findById(item.getBalanceId()).get()
        return MoneyOperItemDto(item.id, item.getBalanceId(), balance.name, item.value.abs(), item.value.signum(),
                item.balance.currencyCode, item.performed, item.index)
    }
}