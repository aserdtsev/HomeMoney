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
        balanceSheet: BalanceSheet,
        type: AccountType,
        name: String,
        created: Date,
        isArc: Boolean? = null,
        root: Category? = null
) : Account(balanceSheet, type, name, created, isArc), Comparable<Account> {
    @get:JsonIgnore
    @ManyToOne
    @JoinColumn(name = "root_id")
    open var root: Category? = root

    @Transient
    open val rootId: UUID? = null

    fun init(categoryRepo: CategoryRepository) {
        super.init()
        root = rootId?.let { categoryRepo.findByIdOrNull(rootId)!! }
    }

    override fun getSortIndex(): String {
        return root?.let { "${it.sortIndex}#$name" } ?: "${type.ordinal}#$name"
    }

    override fun compareTo(other: Account): Int {
        if (other !is Category) {
            return 0
        }
        val typeComparing = type.compareTo(other.type)
        return if (typeComparing != 0) typeComparing else sortIndex.compareTo(other.sortIndex)
    }
}