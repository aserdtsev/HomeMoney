package ru.serdtsev.homemoney.infra.config

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ConversionServiceFactoryBean
import ru.serdtsev.homemoney.port.converter.account.*
import ru.serdtsev.homemoney.port.converter.balancesheet.BsDayStatToDtoConverter
import ru.serdtsev.homemoney.port.converter.balancesheet.BsStatToDtoConverter
import ru.serdtsev.homemoney.port.converter.balancesheet.CategoryStatToDtoConverter
import ru.serdtsev.homemoney.port.converter.moneyoper.*
import java.time.Clock

@Configuration
class ConverterConfiguration : ApplicationContextAware {
    private lateinit var applicationContext: ApplicationContext

    @Bean("conversionService")
    fun getConversionService(clock: Clock): ConversionServiceFactoryBean = ConversionServiceFactoryBean().apply {
        val converters = setOf(
            AccountToDto(),
            BalanceToDto(),
            BalanceDtoToModel(),
            ReserveToDto(),
            ReserveDtoToModel(clock),
            MoneyOperToDtoConverter(applicationContext),
            MoneyOperDtoToModelConverter(applicationContext),
            MoneyOperItemToDtoConverter(applicationContext),
            RecurrenceOperToDtoConverter(applicationContext),
            RecurrenceOperToModelConverter(applicationContext),
            TagToTagDtoConverter(),
            TagDtoToModelConverter(),
            BsStatToDtoConverter(applicationContext),
            CategoryStatToDtoConverter(),
            BsDayStatToDtoConverter()
        )
        setConverters(converters)
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }
}