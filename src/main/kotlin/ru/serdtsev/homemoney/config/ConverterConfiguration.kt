package ru.serdtsev.homemoney.config

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ConversionServiceFactoryBean
import ru.serdtsev.homemoney.account.converter.*
import ru.serdtsev.homemoney.moneyoper.converter.*

@Configuration
class ConverterConfiguration : ApplicationContextAware {
    private lateinit var ctx: ApplicationContext

    @Bean("conversionService")
    fun getConversionService(): ConversionServiceFactoryBean = ConversionServiceFactoryBean().apply {
        val converters = setOf(
            AccountToDto(),
            BalanceToDto(),
            BalanceDtoToModel(ctx),
            ReserveToDto(),
            ReserveDtoToModel(ctx),
            MoneyOperToDto(ctx),
            MoneyOperItemToDto(ctx),
            RecurrenceOperToDto(ctx),
            TagToTagDto(),
            TagDtoToModel(ctx)
        )
        setConverters(converters)
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        ctx = applicationContext
    }
}