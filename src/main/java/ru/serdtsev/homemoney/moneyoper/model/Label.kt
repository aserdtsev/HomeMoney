package ru.serdtsev.homemoney.moneyoper.model

import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "labels")
class Label(
        @Id
        var id: UUID?,

        @ManyToOne
        @JoinColumn(name = "bs_id")
        var balanceSheet: BalanceSheet?,

        var name: String?,

        @Column(name = "root_id")
        var rootId: UUID? = null,

        @Column(name = "is_category")
        var category: Boolean? = null,

        @Column(name = "cat_type")
        @Enumerated(EnumType.STRING)
        var categoryType: CategoryType? = null,

        @Column(name = "is_arc")
        val arc: Boolean? = null
) {
    constructor(id: UUID?, balanceSheet: BalanceSheet?, name: String?) : this(id, balanceSheet, name, null,
            null,null, null)

    fun isCategory() = category ?: false

    fun setCategory(value: Boolean) {
        category = value
    }
}