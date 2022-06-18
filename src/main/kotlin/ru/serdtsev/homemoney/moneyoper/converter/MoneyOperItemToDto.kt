package ru.serdtsev.homemoney.moneyoper.converter

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.account.dao.BalanceDao
import ru.serdtsev.homemoney.moneyoper.dto.MoneyOperItemDto
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperItem

class MoneyOperItemToDto(private val appCtx: ApplicationContext) : Converter<MoneyOperItem, MoneyOperItemDto> {
    private val balanceDao: BalanceDao
        get() = appCtx.getBean(BalanceDao::class.java)

    override fun convert(item: MoneyOperItem): MoneyOperItemDto {
        val balanceId = item.balance.id
        val balance = balanceDao.findById(balanceId)
        return MoneyOperItemDto(item.id, balanceId, balance.name, item.value.abs(), item.value.signum(),
                item.balance.currencyCode, item.performed, item.index)
    }
}