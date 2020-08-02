package ru.serdtsev.homemoney

import net.sf.ehcache.CacheManager
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.servlet.ServletComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

@ServletComponentScan
@Configuration
@EnableAsync
@SpringBootApplication
class HmApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HmApplication::class.java, *args)
        }
    }

    init {
        CacheManager.newInstance()
    }
}