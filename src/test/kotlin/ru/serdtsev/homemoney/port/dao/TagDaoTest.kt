package ru.serdtsev.homemoney.port.dao

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.domain.model.moneyoper.CategoryType
import ru.serdtsev.homemoney.domain.model.moneyoper.Tag
import ru.serdtsev.homemoney.port.dao.TagDao
import java.util.*

internal class TagDaoTest: SpringBootBaseTest() {
    @Autowired
    lateinit var tagDao: TagDao

    @Test
    internal fun crud() {
        val tag = Tag.of("tag-name")

        assertTrue(tagDao.exists(tag.id))
        assertEquals(tag, tagDao.findById(tag.id))

        val rootTag = Tag.of("root-tag-name", CategoryType.income)
        with(tag) {
            name = "new-tag-name"
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
        val tags = listOf(Tag.of("tag_name"))
        val objId = UUID.randomUUID()

        tagDao.updateLinks(tags, objId, "operation")
        assertEquals(tags, tagDao.findByObjId(objId))

        tagDao.updateLinks(listOf(), objId, "operation")
        assertEquals(listOf<Tag>(), tagDao.findByObjId(objId))
    }

    @Test
    internal fun findByBalanceSheetAndName() {
        val name = "tag-name"
        val tag = Tag.of(name)

        tagDao.findOrNullByBalanceSheetAndName(name).also {
            assertEquals(tag, it)
        }

        assertNull(tagDao.findOrNullByBalanceSheetAndName("other-tag-name"))
    }

    @Test
    internal fun findByBalanceSheetOrderByName() {
        val tags = listOf(
            Tag.of("tag-name-3"),
            Tag.of("tag-name-2"),
            Tag.of("tag-name-1"),
        ).reversed()

        val actual = tagDao.findByBalanceSheetOrderByName()

        assertEquals(tags, actual)
    }
}