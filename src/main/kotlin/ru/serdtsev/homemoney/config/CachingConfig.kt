package ru.serdtsev.homemoney.config

import org.springframework.cache.annotation.CachingConfigurerSupport
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.context.annotation.Bean
import ru.serdtsev.homemoney.common.ApiRequestContextHolder


//@EnableCaching
//@Configuration
class CachingConfig(val apiRequestContextHolder: ApiRequestContextHolder) : CachingConfigurerSupport() {
    @Bean
    override fun keyGenerator(): KeyGenerator =
        KeyGenerator { _, _, params ->
            val elements = listOf(ApiRequestContextHolder.requestId).plus(params).toTypedArray()
            SimpleKey(elements)
        }
}
