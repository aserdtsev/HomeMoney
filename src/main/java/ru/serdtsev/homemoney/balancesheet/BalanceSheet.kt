package ru.serdtsev.homemoney.balancesheet

import com.fasterxml.jackson.annotation.JsonIgnore
import ru.serdtsev.homemoney.account.model.*
import java.io.Serializable
import java.sql.Date
import java.time.Instant
import java.time.LocalDate
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "balance_sheets")
data class BalanceSheet(
        @Id
        var id: UUID? = null,

        var created: Instant? = null,

        @Column(name = "currency_code")
        var currencyCode: String? = null,

        @OneToOne @JoinColumn(name = "svc_rsv_id", insertable = false) @get:JsonIgnore
        var svcRsv: ServiceAccount? = null,

        @OneToOne @JoinColumn(name = "uncat_costs_id", insertable = false) @get:JsonIgnore
        var uncatCosts: Category? = null,

        @OneToOne @JoinColumn(name = "uncat_income_id", insertable = false) @get:JsonIgnore
        var uncatIncome: Category? = null,

        @OneToMany
        @JoinColumn(name = "balance_sheet_id")
        @get:JsonIgnore
        var accounts: MutableList<Account>? = null
) : Serializable {
    val balances: List<Balance>?
        @JsonIgnore get() = this.accounts
                ?.filter { account -> account is Balance }
                ?.map { account -> account as Balance }
                .orEmpty()

    fun init(): BalanceSheet {
        val now = Date.valueOf(LocalDate.now())
        svcRsv = ServiceAccount(this, "Service reserve", now, false)
        uncatCosts = Category(this, AccountType.expense, "<Без категории>", now, false, null)
        uncatIncome = Category(this, AccountType.income, "<Без категории>", now, false, null)
        return this
    }

    override fun toString() = "BalanceSheet{id=$id, created=$created, currencyCode=$currencyCode}"

    companion object {
        fun newInstance(): BalanceSheet {
            return BalanceSheet(UUID.randomUUID(), Instant.now(), "RUB").init()
        }
    }
}
