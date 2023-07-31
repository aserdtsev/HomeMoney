package ru.serdtsev.homemoney.domain.model.moneyoper

import ru.serdtsev.homemoney.domain.event.DomainEvent
import java.util.*

class Tag(
    val id: UUID,
    var name: String,
    var rootId: UUID? = null,
    // todo Сделать вычисляемым от categoryType
    var isCategory: Boolean = false,
    var categoryType: CategoryType? = null,
    var arc: Boolean = false
) : DomainEvent {
    constructor(name: String) : this(UUID.randomUUID(), name)


    override fun toString(): String {
        return "Tag(id=$id, name='$name', rootId=$rootId, categoryType=$categoryType, arc=$arc, isCategory=$isCategory)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tag

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}

