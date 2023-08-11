package ru.serdtsev.homemoney.port.converter.moneyoper

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperItem
import ru.serdtsev.homemoney.domain.repository.BalanceRepository
import ru.serdtsev.homemoney.port.dto.moneyoper.MoneyOperItemDto

class MoneyOperItemToDtoConverter(private val applicationContext: ApplicationContext) : Converter<MoneyOperItem, MoneyOperItemDto> {
    private val balanceRepository: BalanceRepository
        get() = applicationContext.getBean(BalanceRepository::class.java)

    override fun convert(item: MoneyOperItem): MoneyOperItemDto {
        val balanceId = item.balance.id
        val balance = balanceRepository.findById(balanceId)
        return MoneyOperItemDto(item.id, balanceId, balance.name, item.value.abs(), item.value.signum(),
                item.balance.currencyCode, item.performed, item.index)
    }
}