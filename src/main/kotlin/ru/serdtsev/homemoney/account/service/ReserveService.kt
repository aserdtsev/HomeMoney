package ru.serdtsev.homemoney.account.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.account.dao.BalanceDao
import ru.serdtsev.homemoney.account.dao.ReserveDao
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.account.model.Reserve
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.dao.MoneyOperDao
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper
import ru.serdtsev.homemoney.moneyoper.service.MoneyOperService
import java.lang.UnsupportedOperationException
import java.util.*

@Service
class ReserveService(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val reserveDao: ReserveDao,
    private val balanceDao: BalanceDao,
    private val moneyOperDao: MoneyOperDao,
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
        Reserve.merge(reserve, origReserve).forEach { model ->
            when (model) {
                is MoneyOper -> moneyOperDao.save(model)
                is Balance -> balanceDao.save(model)
                is Reserve -> reserveDao.save(model)
                else -> throw UnsupportedOperationException()
            }
        }
        reserveDao.save(origReserve)
    }

    @Transactional
    fun deleteOrArchive(reserveId: UUID) {
        balanceService.deleteOrArchiveBalance(reserveId)
    }
}