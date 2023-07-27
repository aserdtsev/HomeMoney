package ru.serdtsev.homemoney.port.converter.account

import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.port.dto.account.ReserveDto
import ru.serdtsev.homemoney.domain.model.account.Reserve

class ReserveToDto : Converter<Reserve, ReserveDto> {
    override fun convert(source: Reserve): ReserveDto {
        return with (source) {
            ReserveDto(id, type, name, createdDate, isArc, currencyCode, value, target)
        }
    }
}