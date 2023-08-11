package ru.serdtsev.homemoney.infra.config

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ConversionServiceFactoryBean
import ru.serdtsev.homemoney.port.converter.account.*
import ru.serdtsev.homemoney.port.converter.moneyoper.*

@Configuration
class ConverterConfiguration : ApplicationContextAware {
    private lateinit var applicationContext: ApplicationContext

    @Bean("conversionService")
    fun getConversionService(): ConversionServiceFactoryBean = ConversionServiceFactoryBean().apply {
        val converters = setOf(
            AccountToDto(),
            BalanceToDto(),
            BalanceDtoToModel(),
            ReserveToDto(),
            ReserveDtoToModel(),
            MoneyOperToDtoConverter(applicationContext),
            MoneyOperDtoToModelConverter(applicationContext),
            MoneyOperItemToDtoConverter(applicationContext),
            RecurrenceOperToDtoConverter(applicationContext),
            RecurrenceOperToModelConverter(applicationContext),
            TagToTagDtoConverter(),
            TagDtoToModelConverter()
        )
        setConverters(converters)
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }
}