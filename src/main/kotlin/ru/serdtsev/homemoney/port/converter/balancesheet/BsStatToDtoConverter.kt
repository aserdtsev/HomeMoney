package ru.serdtsev.homemoney.port.converter.balancesheet

import org.springframework.context.ApplicationContext
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.domain.model.balancesheet.BsDayStat
import ru.serdtsev.homemoney.domain.model.balancesheet.BsStat
import ru.serdtsev.homemoney.domain.model.balancesheet.CategoryStat
import ru.serdtsev.homemoney.port.common.moneyScale
import ru.serdtsev.homemoney.port.dto.balancesheet.BsDayStatDto
import ru.serdtsev.homemoney.port.dto.balancesheet.BsStatDto
import ru.serdtsev.homemoney.port.dto.balancesheet.CategoryStatDto
import java.time.ZoneOffset

class BsStatToDtoConverter(private val applicationContext: ApplicationContext) : Converter<BsStat, BsStatDto> {
    private val conversionService: ConversionService
        get() = applicationContext.getBean("conversionService", ConversionService::class.java)

    override fun convert(source: BsStat): BsStatDto = with (source) {
        val categories = source.categories
            .map { requireNotNull(conversionService.convert(it, CategoryStatDto::class.java)) }
        val dayStats = source.dayStats
            .map { requireNotNull(conversionService.convert(it, BsDayStatDto::class.java)) }
        BsStatDto(fromDate, toDate, moneyScale(debitSaldo), moneyScale(creditSaldo), moneyScale(assetSaldo),
            moneyScale(totalSaldo), moneyScale(reserveSaldo), moneyScale(freeAmount), moneyScale(actualDebt),
            moneyScale(actualCreditCardDebt), moneyScale(incomeAmount), moneyScale(chargesAmount), categories, dayStats)
    }
}

class CategoryStatToDtoConverter : Converter<CategoryStat, CategoryStatDto> {
    override fun convert(source: CategoryStat): CategoryStatDto = with (source) {
        CategoryStatDto(id, name, amount, isReserve)
    }
}

class BsDayStatToDtoConverter : Converter<BsDayStat, BsDayStatDto> {
    override fun convert(source: BsDayStat): BsDayStatDto {
        return with (source) {
            val date = localDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() - 1
            BsDayStatDto(date, moneyScale(totalSaldo), moneyScale(freeAmount), moneyScale(incomeAmount),
                moneyScale(chargeAmount), moneyScale(debt))
        }
    }
}