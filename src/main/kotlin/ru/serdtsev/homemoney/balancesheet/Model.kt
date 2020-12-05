package ru.serdtsev.homemoney.balancesheet

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import ru.serdtsev.homemoney.account.model.Account
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "balance_sheets")
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

data class BsStat(
        val bsId: UUID,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        val fromDate: LocalDate,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        val toDate: LocalDate
) {
    var incomeAmount: BigDecimal = BigDecimal.ZERO
    var chargesAmount: BigDecimal = BigDecimal.ZERO
    var dayStats: List<BsDayStat>? = null
    var categories: List<CategoryStat>? = null

    @JsonIgnore
    val saldoMap = HashMap<AccountType, BigDecimal>()

    @Suppress("unused")
    val freeAmount: BigDecimal
        get() = debitSaldo.subtract(reserveSaldo).plus(creditSaldo)

    @Suppress("MemberVisibilityCanBePrivate")
    val reserveSaldo: BigDecimal
        get() = saldoMap.getOrDefault(AccountType.reserve, BigDecimal.ZERO)

    @Suppress("unused")
    val totalSaldo: BigDecimal
        get() = debitSaldo.add(creditSaldo).add(assetSaldo)

    @Suppress("MemberVisibilityCanBePrivate")
    val debitSaldo: BigDecimal
        get() = getSaldo(AccountType.debit)

    @Suppress("MemberVisibilityCanBePrivate")
    val creditSaldo: BigDecimal
        get() = getSaldo(AccountType.credit)

    @Suppress("MemberVisibilityCanBePrivate")
    val assetSaldo: BigDecimal
        get() = getSaldo(AccountType.asset)

    private fun getSaldo(type: AccountType): BigDecimal {
        return saldoMap.getOrDefault(type, BigDecimal.ZERO)
    }
}

data class BsDayStat(@JsonIgnore val localDate: LocalDate) {
    var incomeAmount: BigDecimal = BigDecimal.ZERO
    var chargeAmount: BigDecimal = BigDecimal.ZERO
    private val saldoMap = HashMap<AccountType, BigDecimal>()
    private val deltaMap = HashMap<AccountType, BigDecimal>()

    // Unix-дата и время конца дня в UTC. Так нужно для визуального компонента.
    val date: Long
        @JsonProperty("date")
        get() = localDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() - 1

    @Suppress("unused")
    val totalSaldo: BigDecimal
        get() = getSaldo(AccountType.debit).add(getSaldo(AccountType.credit)).add(getSaldo(AccountType.asset))

    @Suppress("unused")
    val freeAmount: BigDecimal
        get() = getSaldo(AccountType.debit).subtract(reserveSaldo).plus(creditSaldo)

    private val reserveSaldo: BigDecimal
        get() = getSaldo(AccountType.reserve)

    private val creditSaldo: BigDecimal
        get() = getSaldo(AccountType.credit)

    private fun getSaldo(type: AccountType): BigDecimal {
        return saldoMap.getOrDefault(type, BigDecimal.ZERO)
    }

    fun setSaldo(type: AccountType, value: BigDecimal) {
        saldoMap[type] = value.plus()
    }

    fun getDelta(type: AccountType): BigDecimal {
        return deltaMap.getOrDefault(type, BigDecimal.ZERO)
    }

    fun setDelta(type: AccountType, amount: BigDecimal) {
        deltaMap[type] = amount
    }
}

data class CategoryStat(
        var id: UUID,
        var rootId: UUID?,
        var name: String,
        var amount: BigDecimal) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as CategoryStat?
        return this.id == that!!.id
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id)
    }
}

/**
 * Обороты за день по типу счета
 */
data class Turnover(
        val operDate: LocalDate,
        val accountType: AccountType,
        /** Сумма оборотов со знаком */
        var amount: BigDecimal = BigDecimal.ZERO
) {
    operator fun plus(value: BigDecimal) { amount = amount.add(value) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Turnover

        if (operDate != other.operDate) return false
        if (accountType != other.accountType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = operDate.hashCode()
        result = 31 * result + accountType.hashCode()
        return result
    }
}
