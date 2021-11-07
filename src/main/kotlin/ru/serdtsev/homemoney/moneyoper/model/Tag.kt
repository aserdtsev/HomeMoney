package ru.serdtsev.homemoney.moneyoper.model

import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "tag")
class Tag(
    @Id
        val id: UUID,

    @ManyToOne
        @JoinColumn(name = "balance_sheet_id")
        val balanceSheet: BalanceSheet,

    val name: String,

    @Column(name = "root_id")
        val rootId: UUID? = null,

    isCategory: Boolean? = null,

    @Column(name = "cat_type")
        @Enumerated(EnumType.STRING)
        val categoryType: CategoryType? = null,

    @Column(name = "is_arc")
        val arc: Boolean? = null
) {
    @Column(name = "is_category")
    val isCategory: Boolean? = isCategory
        get() = field ?: false

    constructor(id: UUID, balanceSheet: BalanceSheet, name: String) : this(id, balanceSheet, name, null,
            null, null, null)
}

