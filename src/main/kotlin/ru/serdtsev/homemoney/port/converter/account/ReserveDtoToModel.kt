package ru.serdtsev.homemoney.port.converter.account

import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.port.dto.account.ReserveDto
import ru.serdtsev.homemoney.domain.model.account.Reserve

class ReserveDtoToModel : Converter<ReserveDto, Reserve> {
    override fun convert(source: ReserveDto): Reserve {
        return with (source) {
            Reserve(id, name, createdDate, currencyCode, value, target, isArc)
        }
    }
}