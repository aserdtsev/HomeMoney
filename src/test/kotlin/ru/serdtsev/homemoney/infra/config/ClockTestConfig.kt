package ru.serdtsev.homemoney.infra.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@TestConfiguration
class ClockTestConfig {
    @Bean
    fun clock(): Clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
}