package ru.serdtsev.homemoney.domain.repository

import ru.serdtsev.homemoney.domain.model.moneyoper.Tag
import java.util.*

interface TagRepository {
    fun save(domainAggregate: Tag)
    fun delete(tag: Tag)
    fun updateLinks(tags: Collection<Tag>, objId: UUID, objType: String)
    fun link(tag: Tag, objId: UUID, objType: String)
    fun deleteLinks(objId: UUID)
    fun findById(id: UUID): Tag
    fun findByIdOrNull(id: UUID): Tag?
    fun exists(id: UUID): Boolean
    fun findByBalanceSheetAndName(name: String): Tag?
    fun findByBalanceSheetOrderByName(): List<Tag>
    fun findByObjId(objId: UUID): List<Tag>
}