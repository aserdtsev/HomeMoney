package ru.serdtsev.homemoney.moneyoper.model

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import ru.serdtsev.homemoney.account.BalanceRepository

@Component
class MoneyOperItemToDtoConverter(private val appCtx: ApplicationContext) : Converter<MoneyOperItem, MoneyOperItemDto> {
    private var balanceRepo: BalanceRepository? = null

    override fun convert(item: MoneyOperItem): MoneyOperItemDto {
        val balance = getBalanceRepo().findById(item.getBalanceId()).get()
        return MoneyOperItemDto(item.id, item.getBalanceId(), balance.name, item.value.abs(), item.value.signum(),
                item.balance.currencyCode, item.performed, item.index)
    }

    private fun getBalanceRepo(): BalanceRepository {
        if (balanceRepo == null) balanceRepo = appCtx.getBean(BalanceRepository::class.java)
        return balanceRepo!!
    }
}