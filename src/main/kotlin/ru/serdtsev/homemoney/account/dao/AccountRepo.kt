package ru.serdtsev.homemoney.account

import org.springframework.data.repository.CrudRepository
import ru.serdtsev.homemoney.account.model.Account
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.account.model.Reserve
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import java.util.*

interface AccountRepo : CrudRepository<Account, UUID>

interface BalanceRepo : CrudRepository<Balance, UUID> {
    fun findByBalanceSheet(balanceSheet: BalanceSheet): List<Balance>
}

interface ReserveRepo : CrudRepository<Reserve, UUID> {
    fun findByBalanceSheet(balanceSheet: BalanceSheet): List<Reserve>
}