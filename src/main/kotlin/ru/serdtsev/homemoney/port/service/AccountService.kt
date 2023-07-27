package ru.serdtsev.homemoney.port.service

import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.model.account.Account
import ru.serdtsev.homemoney.domain.repository.BalanceRepository
import ru.serdtsev.homemoney.domain.repository.ReserveRepository
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder

@Service
class AccountService(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val balanceRepository: BalanceRepository,
    private val reserveRepository: ReserveRepository
) {
    fun getAccounts(): List<Account> {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        return balanceRepository.findByBalanceSheet(balanceSheet).plus(reserveRepository.findByBalanceSheet(balanceSheet))
    }
}