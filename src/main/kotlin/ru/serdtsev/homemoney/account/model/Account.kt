package ru.serdtsev.homemoney.account.model

import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.Model
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
) : Model, Serializable {

    open fun getSortIndex(): String = name
    override fun merge(other: Any): Collection<Model> {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Account) return false

        if (id != other.id) return false
        if (balanceSheet != other.balanceSheet) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + balanceSheet.hashCode()
        return result
    }

    override fun toString(): String {
        return "Account(id=$id, balanceSheet=$balanceSheet, type=$type, name='$name', createdDate=$createdDate, isArc=$isArc)"
    }

    companion object {
        fun merge(from: Account, to: Account): Collection<Model> {
            val changeModels = mutableListOf<Model>(to)
            to.type = from.type
            to.name = from.name
            to.createdDate = from.createdDate
            to.isArc = from.isArc
            return changeModels
        }
    }
}