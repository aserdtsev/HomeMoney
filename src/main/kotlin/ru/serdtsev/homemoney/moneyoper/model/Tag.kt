package ru.serdtsev.homemoney.moneyoper.model

import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import java.util.*

class Tag(
    val id: UUID,
    val balanceSheet: BalanceSheet,
    var name: String,
    var rootId: UUID? = null,
    // todo Сделать вычисляемым от categoryType
    var isCategory: Boolean = false,
    var categoryType: CategoryType? = null,
    var arc: Boolean = false
) {
    constructor(balanceSheet: BalanceSheet, name: String) : this(UUID.randomUUID(), balanceSheet, name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tag) return false

        if (id != other.id) return false
        if (balanceSheet != other.balanceSheet) return false
        if (name != other.name) return false
        if (rootId != other.rootId) return false
        if (categoryType != other.categoryType) return false
        if (arc != other.arc) return false
        if (isCategory != other.isCategory) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + balanceSheet.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (rootId?.hashCode() ?: 0)
        result = 31 * result + (categoryType?.hashCode() ?: 0)
        result = 31 * result + (arc.hashCode())
        return result
    }

    override fun toString(): String {
        return "Tag(id=$id, balanceSheet=$balanceSheet, name='$name', rootId=$rootId, categoryType=$categoryType, arc=$arc, isCategory=$isCategory)"
    }

}

