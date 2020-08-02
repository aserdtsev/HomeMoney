package ru.serdtsev.homemoney.account.model

import com.fasterxml.jackson.annotation.JsonIgnore
import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import java.io.Serializable
import java.sql.Date
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "accounts")
@Inheritance(strategy = InheritanceType.JOINED)
open class Account(
        @Id open val id: UUID,
        @get:JsonIgnore @ManyToOne @JoinColumn(name = "balance_sheet_id") open var balanceSheet: BalanceSheet,
        @Enumerated(EnumType.STRING) open var type: AccountType,
        open var name: String,
        @Column(name = "created_date") open var createdDate: Date,
        @Column(name = "is_arc") open var isArc: Boolean? = null
) : Serializable {
    fun merge(account: Account) {
        type = account.type
        name = account.name
        createdDate = account.createdDate
        isArc = account.isArc
    }

    open fun getSortIndex(): String = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val account = other as Account
        return id == account.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }
}