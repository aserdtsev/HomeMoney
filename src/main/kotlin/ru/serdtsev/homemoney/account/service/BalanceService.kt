package ru.serdtsev.homemoney.account.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.account.dao.BalanceDao
import ru.serdtsev.homemoney.account.dao.ReserveDao
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.dao.MoneyOperItemDao
import ru.serdtsev.homemoney.moneyoper.service.MoneyOperService
import java.util.*

@Service
class BalanceService(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val balanceDao: BalanceDao,
    private val moneyOperItemDao: MoneyOperItemDao,
    private val reserveDao: ReserveDao,
    private val moneyOperService: MoneyOperService
) {
    @Transactional(readOnly = true)
    fun getBalances(): List<Balance> {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        return balanceDao.findByBalanceSheet(balanceSheet).filter { it.type != AccountType.reserve }
    }

    @Transactional
    fun createBalance(balance: Balance) {
        balanceDao.save(balance)
    }

    @Transactional
    fun updateBalance(balance: Balance) {
        val origBalance = balanceDao.findByIdOrNull(balance.id)!!
        origBalance.merge(balance, reserveDao, moneyOperService)
        balanceDao.save(origBalance)
    }

    @Transactional
    fun deleteOrArchiveBalance(balanceId: UUID) {
        val balance = balanceDao.findById(balanceId)
        val isOperExist = moneyOperItemDao.findByBalance(balance).take(1).isNotEmpty()
        if (isOperExist) {
            balance.isArc = true
            balanceDao.save(balance)
            log.info { "$balance moved to archive." }
        } else {
            balanceDao.delete(balance)
            log.info { "$balance deleted." }
        }
    }

    @Transactional
    fun upBalance(balanceId: UUID) {
        // todo работает неправильно, исправить
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        val balance = balanceDao.findById(balanceId)
        val balances = balanceDao.findByBalanceSheet(balanceSheet)
            .plus(reserveDao.findByBalanceSheet(balanceSheet))
            .sortedBy { it.num }.toMutableList()
        assert(balances.isNotEmpty())
        var prev: Balance?
        do {
            val index = balances.indexOf(balance)
            assert(index > -1)
            prev = null
            if (index > 0) {
                prev = balances[index - 1]
                balances[index - 1] = balance
                balances[index] = prev
                var i: Long = 0
                for (b in balances) {
                    b.num = i++
                }
            }
        } while (prev != null && prev.isArc)
        balances.forEach { balanceDao.save(it) }
    }

    companion object {
        private val log = KotlinLogging.logger {  }
    }
}