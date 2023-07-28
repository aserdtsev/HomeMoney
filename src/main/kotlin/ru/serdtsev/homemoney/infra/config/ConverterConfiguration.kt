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
    private lateinit var ctx: ApplicationContext

    @Bean("conversionService")
    fun getConversionService(): ConversionServiceFactoryBean = ConversionServiceFactoryBean().apply {
        val converters = setOf(
            AccountToDto(),
            BalanceToDto(),
            BalanceDtoToModel(),
            ReserveToDto(),
            ReserveDtoToModel(),
            MoneyOperToDtoConverter(ctx),
            MoneyOperDtoToModelConverter(ctx),
            MoneyOperItemToDtoConverter(ctx),
            RecurrenceOperToDtoConverter(ctx),
            TagToTagDtoConverter(),
            TagDtoToModelConverter(ctx)
        )
        setConverters(converters)
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        ctx = applicationContext
    }
}