package ru.serdtsev.homemoney.account.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.account.dao.ReserveDao
import ru.serdtsev.homemoney.account.model.Reserve
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.service.MoneyOperService
import java.util.*

@Service
class ReserveService(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val reserveDao: ReserveDao,
    private val moneyOperService: MoneyOperService,
    private val balanceService: BalanceService
) {
    @Transactional(readOnly = true)
    fun getReserves(): List<Reserve> {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        return reserveDao.findByBalanceSheet(balanceSheet)
    }

    @Transactional
    fun create(reserve: Reserve) {
        reserveDao.save(reserve)
    }

    @Transactional
    fun update(reserve: Reserve) {
        val origReserve = reserveDao.findByIdOrNull(reserve.id)!!
        origReserve.merge(reserve, reserveDao, moneyOperService)
        reserveDao.save(origReserve)
    }

    @Transactional
    fun deleteOrArchive(reserveId: UUID) {
        balanceService.deleteOrArchiveBalance(reserveId)
    }
}