package ru.serdtsev.homemoney.moneyoper.dao

import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.serdtsev.homemoney.balancesheet.BalanceSheetDao
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.model.CategoryType
import ru.serdtsev.homemoney.moneyoper.model.Tag
import java.sql.ResultSet
import java.util.*

@Repository
class TagDao(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val balanceSheetDao: BalanceSheetDao
) {
    fun save(tag: Tag) {
        val sql = """
            insert into tag(id, name, is_category, root_id, is_arc, cat_type, balance_sheet_id)
                values(:id, :name, :isCategory, :rootId, :isArc, :categoryType, :bsId)
            on conflict(id) do update set 
                name = :name, is_category = :isCategory, root_id = :rootId, is_arc = :isArc, 
                cat_type = :categoryType, balance_sheet_id = :bsId
        """.trimIndent()
        val paramMap = with(tag) {
            mapOf("id" to id, "name" to name, "isCategory" to isCategory, "rootId" to rootId, "isArc" to arc,
                "categoryType" to categoryType?.toString(), "bsId" to balanceSheet.id)
        }
        jdbcTemplate.update(sql, paramMap)
    }

    fun delete(tag: Tag) {
        val sql = "delete from tag where id = :id "
        jdbcTemplate.update(sql, mapOf("id" to tag.id))
    }

    fun updateLinks(tags: Collection<Tag>, objId: UUID, objType: String) {
        deleteLinks(objId)
        tags.forEach { tag ->
            save(tag)
            link(tag, objId, objType)
        }
    }

    private fun link(tag: Tag, objId: UUID, objType: String) {
        val sql = """
            insert into tag2obj(tag_id, obj_id, obj_type) values(:tagId, :objId, :objType)
        """.trimIndent()
        val paramMap = with(tag) {
            mapOf("tagId" to id, "objId" to objId, "objType" to objType)
        }
        jdbcTemplate.update(sql, paramMap)
    }

    private fun deleteLinks(objId: UUID) {
        val sql = "delete from tag2obj where obj_id = :objId"
        val paramMap = mapOf("objId" to objId)
        jdbcTemplate.update(sql, paramMap)
    }

    fun findById(id: UUID): Tag = findByIdOrNull(id)!!

    fun findByIdOrNull(id: UUID): Tag? {
        val sql = "select * from tag where id = :id"
        return jdbcTemplate.query(sql, mapOf("id" to id), rowMapper).firstOrNull()
    }

    fun exists(id: UUID): Boolean = findByIdOrNull(id) != null

    fun findByBalanceSheetAndName(balanceSheet: BalanceSheet, name: String): Tag? {
        val sql = "select * from tag where balance_sheet_id = :bsId and name = :name"
        val paramMap = mapOf("bsId" to balanceSheet.id, "name" to name)
        return jdbcTemplate.query(sql, paramMap, rowMapper).firstOrNull()
    }

    fun findByBalanceSheetOrderByName(balanceSheet: BalanceSheet): List<Tag> {
        val sql = "select * from tag where balance_sheet_id = :bsId order by name"
        return jdbcTemplate.query(sql, mapOf("bsId" to balanceSheet.id), rowMapper)
    }

    @Cacheable("TagDao.findByObjId")
    fun findByObjId(objId: UUID): List<Tag> {
        val sql = "select t.* from tag2obj t2o, tag t where t2o.obj_id = :objId and t.id = t2o.tag_id"
        return jdbcTemplate.query(sql, mapOf("objId" to objId), rowMapper)
    }

    private val rowMapper: (rs: ResultSet, rowNum: Int) -> Tag = { rs, _ ->
        val id = UUID.fromString(rs.getString("id"))
        val name = rs.getString("name")
        val rootId = rs.getString("root_id")?.let { UUID.fromString(it) }
        val isCategory = rs.getBoolean("is_category")
        val categoryType = rs.getString("cat_type")?.let { CategoryType.valueOf(it) }
        val isArc = rs.getBoolean("is_arc") ?: false
        val balanceSheet = rs.getString("balance_sheet_id")
            .let { UUID.fromString(it) }
            .let { balanceSheetDao.findById(it) }
        Tag(id, balanceSheet, name, rootId, isCategory, categoryType, isArc)
    }
}