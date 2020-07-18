package ru.serdtsev.homemoney.moneyoper.model

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import ru.serdtsev.homemoney.account.BalanceRepository

@Component
class MoneyOperItemToDtoConverter(private val appCtx: ApplicationContext) : Converter<MoneyOperItem, MoneyOperItemDto> {
    private var balanceRepo: BalanceRepository? = null

    override fun convert(item: MoneyOperItem): MoneyOperItemDto {
        if (balanceRepo == null) balanceRepo = appCtx.getBean(BalanceRepository::class.java)
        val balance = balanceRepo!!.findById(item.getBalanceId()).get()
        return MoneyOperItemDto(item.id, item.getBalanceId(), balance.name, item.value,
                item.performed, item.index)
    }
}