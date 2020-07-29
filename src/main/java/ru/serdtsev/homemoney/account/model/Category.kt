package ru.serdtsev.homemoney.account.model

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.repository.findByIdOrNull
import ru.serdtsev.homemoney.account.CategoryRepository
import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import java.sql.Date
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "categories")
@DiscriminatorValue("category")
open class Category(
        id: UUID,
        balanceSheet: BalanceSheet,
        type: AccountType,
        name: String,
        created: Date,
        isArc: Boolean? = null,
        @JsonIgnore @ManyToOne @JoinColumn(name = "root_id") open var root: Category? = null
) : Account(id, balanceSheet, type, name, created, isArc), Comparable<Account> {

    @Transient
    open val rootId: UUID? = null

    fun init(categoryRepo: CategoryRepository) {
        root = rootId?.let { categoryRepo.findByIdOrNull(rootId)!! }
    }

    override fun getSortIndex(): String {
        return root?.let { "${it.getSortIndex()}#$name" } ?: "${type.ordinal}#$name"
    }

    override fun compareTo(other: Account): Int {
        if (other !is Category) {
            return 0
        }
        val typeComparing = type.compareTo(other.type)
        return if (typeComparing != 0) typeComparing else getSortIndex().compareTo(other.getSortIndex())
    }
}