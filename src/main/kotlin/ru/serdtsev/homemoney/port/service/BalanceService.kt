package ru.serdtsev.homemoney.port.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.repository.BalanceRepository
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import java.util.*

@Service
class BalanceService(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val balanceRepository: BalanceRepository,
    private val moneyOperRepository: MoneyOperRepository
) {
    @Transactional(readOnly = true)
    fun getBalances(): List<Balance> {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        return balanceRepository.findByBalanceSheet(balanceSheet).filter { it.type != AccountType.reserve }
    }

    @Transactional
    fun createBalance(balance: Balance) {
        DomainEventPublisher.instance.publish(balance)
    }

    @Transactional
    fun updateBalance(balance: Balance) {
        val origBalance = balanceRepository.findByIdOrNull(balance.id)!!
        Balance.merge(balance, origBalance)
    }

    @Transactional
    fun deleteOrArchiveBalance(balanceId: UUID) {
        val balance = balanceRepository.findById(balanceId)
        if (moneyOperRepository.existsByBalance(balance)) {
            balance.isArc = true
            DomainEventPublisher.instance.publish(balance)
            log.info { "$balance moved to archive." }
        } else {
            balanceRepository.delete(balance)
            log.info { "$balance deleted." }
        }
    }

    @Transactional
    fun upBalance(balanceId: UUID) {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        val balance = balanceRepository.findById(balanceId)
        val balances = balanceRepository.findByBalanceSheet(balanceSheet)
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
                    DomainEventPublisher.instance.publish(it)
                }
            } else {
                DomainEventPublisher.instance.publish(balance)
                DomainEventPublisher.instance.publish(prev)
            }
        }
    }

    companion object {
        private val log = KotlinLogging.logger {  }
    }
}