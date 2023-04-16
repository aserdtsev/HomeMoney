package ru.serdtsev.homemoney.moneyoper.dao

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.model.CategoryType
import ru.serdtsev.homemoney.moneyoper.model.Tag
import java.util.*

internal class TagDaoTest: SpringBootBaseTest() {
    @Autowired
    lateinit var tagDao: TagDao

    @Test
    internal fun crud() {
        val balanceSheet = createBalanceSheet()

        val tag = createTag(balanceSheet, "tag-name")

        assertTrue(tagDao.exists(tag.id))
        assertEquals(tag, tagDao.findById(tag.id))

        val rootTag = Tag(balanceSheet, "root-tag-name").apply {
            isCategory = true
            categoryType = CategoryType.income
        }
        tagDao.save(rootTag)
        with(tag) {
            name = "new-tag-name"
            isCategory = true
            rootId = rootTag.id
            categoryType = CategoryType.income
            arc = true
        }
        tagDao.save(tag)
        assertEquals(tag, tagDao.findById(tag.id))

        tagDao.delete(tag)
        assertFalse(tagDao.exists(tag.id))
    }

    @Test
    internal fun `link, findByObjId and unlinkAll`() {
        val balanceSheet = createBalanceSheet()
        val tags = listOf(createTag(balanceSheet, "tag_name"))
        val objId = UUID.randomUUID()

        tagDao.updateLinks(tags, objId, "operation")
        assertEquals(tags, tagDao.findByObjId(objId))

        tagDao.updateLinks(listOf(), objId, "operation")
        assertEquals(listOf<Tag>(), tagDao.findByObjId(objId))
    }

    @Test
    internal fun findByBalanceSheetAndName() {
        val balanceSheet = createBalanceSheet()
        val name = "tag-name"
        val tag = createTag(balanceSheet, name)

        tagDao.findByBalanceSheetAndName(balanceSheet, name).also {
            assertEquals(tag, it)
        }

        assertNull(tagDao.findByBalanceSheetAndName(balanceSheet, "other-tag-name"))
    }

    @Test
    internal fun findByBalanceSheetOrderByName() {
        val balanceSheet = createBalanceSheet()
        val tags = listOf(
            createTag(balanceSheet, "tag-name-3"),
            createTag(balanceSheet, "tag-name-2"),
            createTag(balanceSheet, "tag-name-1"),
        ).reversed()

        val actual = tagDao.findByBalanceSheetOrderByName(balanceSheet)

        assertEquals(tags, actual)
    }

    private fun createTag(balanceSheet: BalanceSheet, name: String): Tag =
        Tag(balanceSheet, name).apply { tagDao.save(this) }

    private fun createBalanceSheet(): BalanceSheet = BalanceSheet().apply {
        balanceSheetDao.save(this)
        ApiRequestContextHolder.bsId = id
    }
}