package ru.serdtsev.homemoney.domain.model.account

import ru.serdtsev.homemoney.domain.event.DomainEvent
import java.io.Serializable
import java.time.LocalDate
import java.util.*

open class Account(
    open val id: UUID,
    open var type: AccountType,
    open var name: String,
    open var createdDate: LocalDate = LocalDate.now(),
    open var isArc: Boolean = false
) : DomainEvent, Serializable {

    open fun getSortIndex(): String = name

    override fun toString(): String {
        return "Account(id=$id, type=$type, name='$name', createdDate=$createdDate, isArc=$isArc)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Account

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        fun merge(from: Account, to: Account): Collection<DomainEvent> {
            val changeDomainModels = mutableListOf<DomainEvent>(to)
            to.type = from.type
            to.name = from.name
            to.createdDate = from.createdDate
            to.isArc = from.isArc
            return changeDomainModels
        }
    }
}