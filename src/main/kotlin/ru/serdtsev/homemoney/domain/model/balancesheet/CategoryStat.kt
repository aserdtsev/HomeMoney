package ru.serdtsev.homemoney.domain.model.balancesheet

import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.moneyoper.Tag
import java.math.BigDecimal
import java.util.*

data class CategoryStat private constructor(
    val id: UUID,
    val name: String,
    var amount: BigDecimal,
    val isReserve: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as CategoryStat?
        return this.id == that!!.id
    }

    override fun hashCode(): Int = Objects.hash(this.id)

    companion object {
        private val absentCatId = UUID(0L, 0L)
        private val absentCatName = "<Без категории>"

        fun of(categoryTag: Tag, amount: BigDecimal): CategoryStat {
            require(categoryTag.isCategory) { "$categoryTag is not category" }
            return CategoryStat(categoryTag.id, categoryTag.name, amount, false)
        }

        fun of(balance: Balance, amount: BigDecimal): CategoryStat {
            require(balance.type == AccountType.reserve) { "$balance is not of type ${AccountType.reserve}" }
            return CategoryStat(balance.id, balance.name, amount, true)
        }

        fun ofAbsentCategory(amount: BigDecimal): CategoryStat = CategoryStat(absentCatId, absentCatName, amount, false)
    }
}