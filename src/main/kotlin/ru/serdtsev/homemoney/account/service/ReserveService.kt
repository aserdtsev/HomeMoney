package ru.serdtsev.homemoney.account.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.account.ReserveRepo
import ru.serdtsev.homemoney.account.model.Reserve
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.service.MoneyOperService
import java.util.*

@Service
class ReserveService(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val reserveRepo: ReserveRepo,
    private val moneyOperService: MoneyOperService,
    private val balanceService: BalanceService
) {
    @Transactional(readOnly = true)
    fun getReserves(): List<Reserve> {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        return reserveRepo.findByBalanceSheet(balanceSheet)
    }

    @Transactional
    fun create(reserve: Reserve) {
        reserveRepo.save(reserve)
    }

    @Transactional
    fun update(reserve: Reserve) {
        val origReserve = reserveRepo.findByIdOrNull(reserve.id)!!
        origReserve.merge(reserve, reserveRepo, moneyOperService)
        reserveRepo.save(origReserve)
    }

    @Transactional
    fun deleteOrArchive(reserveId: UUID) {
        balanceService.deleteOrArchiveBalance(reserveId)
    }
}