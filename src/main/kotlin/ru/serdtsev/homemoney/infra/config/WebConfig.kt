package ru.serdtsev.homemoney.infra.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.RequestContextFilter

@Configuration
class WebConfig {
    @Bean
    fun requestContextFilter(): RequestContextFilter {
        return RequestContextFilter()
    }
}