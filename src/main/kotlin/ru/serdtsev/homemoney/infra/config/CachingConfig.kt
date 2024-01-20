package ru.serdtsev.homemoney.infra.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder


@EnableCaching
@Configuration
class CachingConfig {
    @Bean
    fun keyGenerator(): KeyGenerator =
        KeyGenerator { _, _, params ->
            val elements = listOf(ApiRequestContextHolder.requestId).plus(params).toTypedArray()
            SimpleKey(elements)
        }

    @Bean("firstLevelCacheManager")
//    @RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
    fun getCacheManager(): CacheManager? = ConcurrentMapCacheManager()
}
