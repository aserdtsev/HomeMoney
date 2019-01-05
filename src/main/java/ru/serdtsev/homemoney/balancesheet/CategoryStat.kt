package ru.serdtsev.homemoney.balancesheet

import java.math.BigDecimal
import java.util.*

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
