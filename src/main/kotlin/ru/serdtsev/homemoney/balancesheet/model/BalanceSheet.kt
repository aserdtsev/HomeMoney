package ru.serdtsev.homemoney.balancesheet.model

import com.fasterxml.jackson.annotation.JsonIgnore
import ru.serdtsev.homemoney.account.model.Account
import ru.serdtsev.homemoney.account.model.Balance
import java.io.Serializable
import java.time.Instant
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "balance_sheet")
data class BalanceSheet(
    @Id
        val id: UUID,

    val createdTs: Instant,

    @Column(name = "currency_code")
        var currencyCode: String,

    @OneToMany
        @JoinColumn(name = "balance_sheet_id")
        @get:JsonIgnore
        var accounts: MutableList<Account>? = null
) : Serializable {
    val balances: List<Balance>
        @JsonIgnore get() = this.accounts
                ?.filterIsInstance<Balance>()
                .orEmpty()

    override fun toString() = "BalanceSheet{id=$id, created=$createdTs, currencyCode=$currencyCode}"

    companion object {
        fun newInstance(): BalanceSheet {
            return BalanceSheet(UUID.randomUUID(), Instant.now(), "RUB")
        }
    }
}