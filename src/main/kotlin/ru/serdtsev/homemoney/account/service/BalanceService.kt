package ru.serdtsev.homemoney.account.service

import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.account.BalanceRepo
import ru.serdtsev.homemoney.account.ReserveRepo
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.service.MoneyOperService
import ru.serdtsev.homemoney.moneyoper.dao.MoneyOperItemRepo
import java.util.*

@Service
class BalanceService(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val balanceRepo: BalanceRepo,
    private val moneyOperItemRepo: MoneyOperItemRepo,
    private val reserveRepo: ReserveRepo,
    private val moneyOperService: MoneyOperService
) {
    @Transactional(readOnly = true)
    fun getBalances(): List<Balance> {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        return balanceRepo.findByBalanceSheet(balanceSheet).filter { it.type != AccountType.reserve }
    }

    @Transactional
    fun createBalance(balance: Balance) {
        balance.init(reserveRepo)
        balanceRepo.save(balance)
    }

    @Transactional
    fun updateBalance(balance: Balance) {
        val origBalance = balanceRepo.findByIdOrNull(balance.id)!!
        origBalance.merge(balance, reserveRepo, moneyOperService)
        balanceRepo.save(origBalance)
    }

    @Transactional
    fun deleteOrArchiveBalance(balanceId: UUID) {
        val balance = balanceRepo.findByIdOrNull(balanceId)!!
        val isOperExist = moneyOperItemRepo.findByBalance(balance).take(1).isNotEmpty()
        if (isOperExist) {
            balance.isArc = true
            balanceRepo.save(balance)
            log.info { "$balance moved to archive." }
        } else {
            balanceRepo.delete(balance)
            log.info { "$balance deleted." }
        }
    }

    @Transactional
    fun upBalance(balanceId: UUID) {
        // todo работает неправильно, исправить
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        val balance = balanceRepo.findByIdOrNull(balanceId)!!
        val balances = balanceSheet.balances.sortedBy { it.num }.toMutableList()
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
        } while (prev != null && prev.isArc!!)
        balances.forEach { balanceRepo.save(it) }
    }

    companion object {
        private val log = KotlinLogging.logger {  }
    }
}