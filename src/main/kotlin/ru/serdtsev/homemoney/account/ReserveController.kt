package ru.serdtsev.homemoney.account

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.account.dto.ReserveDto
import ru.serdtsev.homemoney.account.model.Reserve
import ru.serdtsev.homemoney.account.service.ReserveService
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.common.HmResponse.Companion.getOk

@RestController
@RequestMapping("/api/reserves")
class ReserveController(
    private val reserveService: ReserveService,
    @Qualifier("conversionService") private val conversionService: ConversionService
) {
    @RequestMapping
    fun getList(): HmResponse {
        val list = reserveService.getReserves()
            .sortedBy { it.createdDate }
            .map { conversionService.convert(it, ReserveDto::class.java) }
        return getOk(list)
    }

    @RequestMapping("/create")
    fun create(@RequestBody reserveDto: ReserveDto): HmResponse {
        val reserve = conversionService.convert(reserveDto, Reserve::class.java) as Reserve
        reserveService.create(reserve)
        return getOk()
    }

    @RequestMapping("/update")
    fun update(@RequestBody reserveDto: ReserveDto): HmResponse {
        val reserve = conversionService.convert(reserveDto, Reserve::class.java)!!
        reserveService.update(reserve)
        return getOk()
    }

    @RequestMapping("/delete")
    @Transactional
    fun deleteOrArchive(@RequestBody reserveDto: ReserveDto): HmResponse {
        reserveService.deleteOrArchive(reserveDto.id)
        return getOk()
    }
}