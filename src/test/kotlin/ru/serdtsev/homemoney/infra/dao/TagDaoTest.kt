package ru.serdtsev.homemoney.infra.dao

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.domain.model.moneyoper.CategoryType
import ru.serdtsev.homemoney.domain.model.moneyoper.Tag
import java.util.*

internal class TagDaoTest: SpringBootBaseTest() {
    @Autowired
    lateinit var tagDao: TagDao

    @Test
    internal fun crud() {
        val tag = createTag("tag-name")

        assertTrue(tagDao.exists(tag.id))
        assertEquals(tag, tagDao.findById(tag.id))

        val rootTag = Tag("root-tag-name").apply {
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
        val tags = listOf(createTag("tag_name"))
        val objId = UUID.randomUUID()

        tagDao.updateLinks(tags, objId, "operation")
        assertEquals(tags, tagDao.findByObjId(objId))

        tagDao.updateLinks(listOf(), objId, "operation")
        assertEquals(listOf<Tag>(), tagDao.findByObjId(objId))
    }

    @Test
    internal fun findByBalanceSheetAndName() {
        val name = "tag-name"
        val tag = createTag(name)

        tagDao.findByBalanceSheetAndName(balanceSheet, name).also {
            assertEquals(tag, it)
        }

        assertNull(tagDao.findByBalanceSheetAndName(balanceSheet, "other-tag-name"))
    }

    @Test
    internal fun findByBalanceSheetOrderByName() {
        val tags = listOf(
            createTag("tag-name-3"),
            createTag("tag-name-2"),
            createTag("tag-name-1"),
        ).reversed()

        val actual = tagDao.findByBalanceSheetOrderByName(balanceSheet)

        assertEquals(tags, actual)
    }

    private fun createTag(name: String): Tag = Tag(name).apply { tagDao.save(this) }
}