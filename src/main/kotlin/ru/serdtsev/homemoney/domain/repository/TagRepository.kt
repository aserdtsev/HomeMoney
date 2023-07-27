package ru.serdtsev.homemoney.domain.repository

import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
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
    fun findByBalanceSheetAndName(balanceSheet: BalanceSheet, name: String): Tag?
    fun findByBalanceSheetOrderByName(balanceSheet: BalanceSheet): List<Tag>
    fun findByObjId(objId: UUID): List<Tag>
}