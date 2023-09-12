package ru.serdtsev.homemoney.domain.model.moneyoper

import ru.serdtsev.homemoney.domain.event.DomainEvent
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import java.util.*

class Tag(
    val id: UUID,
    var name: String,
    var categoryType: CategoryType?,
    var rootId: UUID?,
    var arc: Boolean = false
) : DomainEvent {
    val isCategory: Boolean
        get() = categoryType != null

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

    companion object {
        fun of(name: String, categoryType: CategoryType? = null, rootId: UUID? = null, arc: Boolean = false,
            id: UUID = UUID.randomUUID()): Tag {
            return Tag(id, name, categoryType, rootId, arc).apply {
                DomainEventPublisher.instance.publish(this)
            }
        }
    }
}

