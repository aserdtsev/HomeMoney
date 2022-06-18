package ru.serdtsev.homemoney.account.converter

import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.account.dto.ReserveDto
import ru.serdtsev.homemoney.account.model.Reserve

class ReserveToDto : Converter<Reserve, ReserveDto> {
    override fun convert(source: Reserve): ReserveDto {
        return with (source) {
            ReserveDto(id, type, name, createdDate, isArc, currencyCode, value, target)
        }
    }
}