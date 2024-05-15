package ru.serdtsev.homemoney.infra.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit


@EnableCaching
@Configuration
class CachingConfig {
    @Bean
    fun cacheManager(caffeine: Caffeine<Any, Any>): CacheManager = CaffeineCacheManager().apply {
        // Common configuration
        setCaffeine(caffeine)
        // Custom configurations
        registerCustomCache("Balance",
            Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(15, TimeUnit.SECONDS)
                .build())
    }

    @Bean
    fun caffeineConfig(): Caffeine<*, *> {
        return Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(60, TimeUnit.MINUTES)
    }
}
