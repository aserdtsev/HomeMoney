package ru.serdtsev.homemoney.port.common

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import ru.serdtsev.homemoney.port.dto.moneyoper.DayRecurrenceParamsDto
import ru.serdtsev.homemoney.port.dto.moneyoper.IRecurrenceParamsDto
import ru.serdtsev.homemoney.port.dto.moneyoper.RecurrenceParamsDto

class RecurrenceParamsDtoDeserializer: JsonDeserializer<RecurrenceParamsDto>() {
    override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): RecurrenceParamsDto {
        val mapper = jsonParser.codec as ObjectMapper
        val root = mapper.readTree(jsonParser) as ObjectNode
        val data = (if (root.has("type")) {
            val type = root.get("type").asText()
            val data = root.get("data")
            when (type) {
                "DayRecurrenceParamsDto" -> mapper.convertValue(data, DayRecurrenceParamsDto::class.java)
                else -> null
            }
        } else null) as IRecurrenceParamsDto
        return RecurrenceParamsDto(data::class.simpleName!!, data)
    }
}