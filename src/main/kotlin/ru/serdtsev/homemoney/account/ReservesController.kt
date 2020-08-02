package ru.serdtsev.homemoney.account

import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Reserve
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository
import ru.serdtsev.homemoney.common.HmException
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.common.HmResponse.Companion.getFail
import ru.serdtsev.homemoney.common.HmResponse.Companion.getOk
import ru.serdtsev.homemoney.moneyoper.MoneyOperService
import java.sql.Date
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/{bsId}/reserves")
class ReservesController(
        private val reserveRepo: ReserveRepository,
        private val balanceSheetRepo: BalanceSheetRepository,
        private val moneyOperService: MoneyOperService,
        private val balanceService: BalanceService
) {
    @RequestMapping
    fun getReserveList(@PathVariable bsId: UUID): HmResponse {
        val balanceSheet = balanceSheetRepo.findByIdOrNull(bsId)!!
        val reserves = reserveRepo.findByBalanceSheet(balanceSheet).sortedBy { it.createdDate }
        return getOk(reserves)
    }

    @RequestMapping("/create")
    @Transactional
    fun createReserve(
            @PathVariable bsId: UUID,
            @RequestBody reserve: Reserve
    ): HmResponse {
        return try {
            val balanceSheet = balanceSheetRepo.findByIdOrNull(bsId)!!
            with (reserve) {
                this.balanceSheet = balanceSheet
                this.type = AccountType.reserve
                this.currencyCode = balanceSheet.currencyCode
                this.createdDate = Date.valueOf(LocalDate.now())
                reserveRepo.save(reserve)
            }
            getOk()
        } catch (e: HmException) {
            getFail(e.code.name)
        }
    }

    @RequestMapping("/update")
    @Transactional
    fun updateReserve(
            @PathVariable bsId: UUID?,
            @RequestBody reserve: Reserve
    ): HmResponse {
        return try {
            val currReserve = reserveRepo.findByIdOrNull(reserve.id)!!
            currReserve.merge(reserve, reserveRepo, moneyOperService)
            reserveRepo.save(currReserve)
            getOk()
        } catch (e: HmException) {
            getFail(e.code.name)
        }
    }

    @RequestMapping("/delete")
    @Transactional
    fun deleteOrArchiveReserve(
            @PathVariable bsId: UUID,
            @RequestBody reserve: Reserve
    ): HmResponse {
        return try {
            balanceService.deleteOrArchiveBalance(reserve.id)
            getOk()
        } catch (e: HmException) {
            getFail(e.code.name)
        }
    }

}