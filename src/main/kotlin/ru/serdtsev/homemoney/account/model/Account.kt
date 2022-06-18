package ru.serdtsev.homemoney.account.model

import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import java.io.Serializable
import java.time.LocalDate
import java.util.*

open class Account(
    open val id: UUID,
    open var balanceSheet: BalanceSheet,
    open var type: AccountType,
    open var name: String,
    open var createdDate: LocalDate = LocalDate.now(),
    open var isArc: Boolean = false
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
        if (other !is Account) return false

        if (id != other.id) return false
        if (balanceSheet != other.balanceSheet) return false
        if (type != other.type) return false
        if (name != other.name) return false
        if (createdDate != other.createdDate) return false
        if (isArc != other.isArc) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + balanceSheet.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + createdDate.hashCode()
        result = 31 * result + isArc.hashCode()
        return result
    }

    override fun toString(): String {
        return "Account(id=$id, balanceSheet=$balanceSheet, type=$type, name='$name', createdDate=$createdDate, isArc=$isArc)"
    }
}