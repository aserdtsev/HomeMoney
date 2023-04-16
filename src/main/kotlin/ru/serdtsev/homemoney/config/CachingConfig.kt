package ru.serdtsev.homemoney.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.web.context.annotation.RequestScope
import ru.serdtsev.homemoney.common.ApiRequestContextHolder


@EnableCaching
@Configuration
class CachingConfig(val apiRequestContextHolder: ApiRequestContextHolder) {
//    @Bean
//    override fun keyGenerator(): KeyGenerator =
//        KeyGenerator { _, _, params ->
//            val elements = listOf(ApiRequestContextHolder.requestId).plus(params).toTypedArray()
//            SimpleKey(elements)
//        }
    @Bean("firstLevelCacheManager")
    @RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
    fun getCacheManager(): CacheManager? = ConcurrentMapCacheManager()
}
