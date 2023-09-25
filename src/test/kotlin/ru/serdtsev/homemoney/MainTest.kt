package ru.serdtsev.homemoney

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import ru.serdtsev.homemoney.infra.config.ClockConfig

@ComponentScan(excludeFilters = [
    ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = [Main::class, ClockConfig::class])
])
@SpringBootApplication
class MainTest {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(MainTest::class.java, *args)
        }
    }
}