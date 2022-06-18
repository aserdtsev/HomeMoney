package ru.serdtsev.homemoney.moneyoper.dao

import ru.serdtsev.homemoney.utils.TestHelper
import com.opentable.db.postgres.junit5.PreparedDbExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import ru.serdtsev.homemoney.account.dao.BalanceDao
import ru.serdtsev.homemoney.account.dao.ReserveDao
import ru.serdtsev.homemoney.balancesheet.BalanceSheetDao
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.model.CategoryType
import ru.serdtsev.homemoney.moneyoper.model.Tag
import java.util.*

internal class TagDaoTest {
    companion object {
        @JvmField @RegisterExtension
        val db: PreparedDbExtension = TestHelper.db
    }

    lateinit var balanceSheetDao: BalanceSheetDao
    lateinit var tagDao: TagDao;
    lateinit var reserveDao: ReserveDao
    lateinit var balanceDao: BalanceDao

    @BeforeEach
    internal fun setUp() {
        val jdbcTemplate = NamedParameterJdbcTemplate(db.testDatabase)
        balanceSheetDao = BalanceSheetDao(jdbcTemplate)
        tagDao = TagDao(jdbcTemplate, balanceSheetDao)
        reserveDao = ReserveDao(jdbcTemplate, balanceSheetDao)
        balanceDao = BalanceDao(jdbcTemplate, balanceSheetDao, reserveDao)
    }

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

        tagDao.updateLinks(listOf<Tag>(), objId, "operation")
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