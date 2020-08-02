package ru.serdtsev.homemoney.config

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ConversionServiceFactoryBean
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperItemToDtoConverter
import java.util.*

@Configuration
class ConverterConfiguration : ApplicationContextAware {
    private lateinit var ctx: ApplicationContext

    @get:Bean(name = ["conversionService"])
    val conversionService: ConversionServiceFactoryBean
        get() {
            val bean = ConversionServiceFactoryBean()
            val converters = HashSet<Any>()
            converters.add(MoneyOperItemToDtoConverter(ctx))
            bean.setConverters(converters)
            return bean
        }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        ctx = applicationContext
    }
}