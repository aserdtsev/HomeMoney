package ru.serdtsev.homemoney.account

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.account.dto.ReserveDto
import ru.serdtsev.homemoney.account.model.Reserve
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.common.HmException
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.common.HmResponse.Companion.getFail
import ru.serdtsev.homemoney.common.HmResponse.Companion.getOk
import ru.serdtsev.homemoney.moneyoper.MoneyOperService

@RestController
@RequestMapping("/api/reserves")
class ReserveController(
        private val apiRequestContextHolder: ApiRequestContextHolder,
        private val reserveRepo: ReserveRepository,
        private val moneyOperService: MoneyOperService,
        private val balanceService: BalanceService,
        @Qualifier("conversionService") private val conversionService: ConversionService
) {
    @RequestMapping
    fun getReserveList(): HmResponse {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        val reserves = reserveRepo.findByBalanceSheet(balanceSheet)
            .sortedBy { it.createdDate }
            .map { conversionService.convert(it, ReserveDto::class.java) }
        return getOk(reserves)
    }

    @RequestMapping("/create")
    @Transactional
    fun createReserve(@RequestBody reserveDto: ReserveDto): HmResponse {
        return try {
            val reserve = conversionService.convert(reserveDto, Reserve::class.java) as Reserve
            reserveRepo.save(reserve)
            getOk()
        } catch (e: HmException) {
            getFail(e.code.name)
        }
    }

    @RequestMapping("/update")
    @Transactional
    fun updateReserve(@RequestBody reserveDto: ReserveDto): HmResponse {
        return try {
            val origReserve = reserveRepo.findByIdOrNull(reserveDto.id)!!
            val reserve = conversionService.convert(reserveDto, Reserve::class.java)!!
            origReserve.merge(reserve, reserveRepo, moneyOperService)
            reserveRepo.save(origReserve)
            getOk()
        } catch (e: HmException) {
            getFail(e.code.name)
        }
    }

    @RequestMapping("/delete")
    @Transactional
    fun deleteOrArchiveReserve(@RequestBody reserveDto: ReserveDto): HmResponse {
        return try {
            balanceService.deleteOrArchiveBalance(reserveDto.id)
            getOk()
        } catch (e: HmException) {
            getFail(e.code.name)
        }
    }

}