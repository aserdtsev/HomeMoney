package ru.serdtsev.homemoney.domain.model.balancesheet

import java.math.BigDecimal
import java.util.*

data class CategoryStat(
    val id: UUID,
    val isReserve: Boolean,
    val name: String,
    var amount: BigDecimal
) {
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