package ru.serdtsev.homemoney.moneyoper.model

import org.springframework.core.convert.converter.Converter
import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "tag")
class Tag(
        @Id
        val id: UUID,

        @ManyToOne
        @JoinColumn(name = "bs_id")
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

class TagDto(val id: UUID, val name: String, val rootId: UUID?, val isCategory: Boolean?, val categoryType: CategoryType?,
             val isArc: Boolean?)

class TagToTagDtoConverter: Converter<Tag, TagDto> {
    override fun convert(source: Tag): TagDto = TagDto(source.id, source.name, source.rootId, source.isCategory,
        source.categoryType, source.arc)
}
