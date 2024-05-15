package ru.serdtsev.homemoney.infra.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import ru.serdtsev.homemoney.port.common.RecurrenceParamsDtoDeserializer
import ru.serdtsev.homemoney.port.dto.moneyoper.RecurrenceParamsDto

@Configuration
class JsonConfig {
    @Autowired
    lateinit var objectMapper: ObjectMapper

    @PostConstruct
    fun init() {
        val module = SimpleModule().apply {
            addDeserializer(RecurrenceParamsDto::class.java, RecurrenceParamsDtoDeserializer())
        }
        objectMapper.registerModule(module)
    }
}