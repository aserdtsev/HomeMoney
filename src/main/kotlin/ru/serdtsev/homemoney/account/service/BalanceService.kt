package ru.serdtsev.homemoney.account.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.account.dao.BalanceDao
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.dao.MoneyOperDao
import ru.serdtsev.homemoney.moneyoper.dao.MoneyOperItemDao
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper
import java.util.*

@Service
class BalanceService(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val balanceDao: BalanceDao,
    private val moneyOperItemDao: MoneyOperItemDao,
    private val moneyOperDao: MoneyOperDao
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
        Balance.merge(balance, origBalance).forEach { model ->
            when (model) {
                is MoneyOper -> moneyOperDao.save(model)
                is Balance -> balanceDao.save(model)
                else -> throw UnsupportedOperationException()
            }
        }
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
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        val balance = balanceDao.findById(balanceId)
        val balances = balanceDao.findByBalanceSheet(balanceSheet)
            .sortedBy { it.num }.toMutableList()
        assert(balances.isNotEmpty())
        val index = balances.indexOf(balance)
        assert(index > -1)
        if (index > 0) {
            val prev = balances[index - 1]
            balances[index - 1] = balance
            balances[index] = prev
            balance.num.let {
                balance.num = prev.num
                prev.num = it
            }
            if (balance.num == prev.num) {
                var i: Long = 0
                balances.forEach {
                    it.num = i++
                    balanceDao.save(it)
                }
            } else {
                balanceDao.save(balance)
                balanceDao.save(prev)
            }
        }
    }

    companion object {
        private val log = KotlinLogging.logger {  }
    }
}