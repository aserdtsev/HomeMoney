package ru.serdtsev.homemoney.account

import org.springframework.data.repository.CrudRepository
import ru.serdtsev.homemoney.account.model.Account
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.account.model.Reserve
import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import java.util.*

interface AccountRepository : CrudRepository<Account, UUID>

interface BalanceRepository : CrudRepository<Balance, UUID> {
    fun findByBalanceSheet(balanceSheet: BalanceSheet): List<Balance>
}

interface ReserveRepository : CrudRepository<Reserve, UUID> {
    fun findByBalanceSheet(balanceSheet: BalanceSheet): List<Reserve>
}