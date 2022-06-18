package ru.serdtsev.homemoney.moneyoper.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.dao.TagDao
import ru.serdtsev.homemoney.moneyoper.model.CategoryType
import ru.serdtsev.homemoney.moneyoper.model.Tag

@Service
@Transactional
class TagService(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val tagDao: TagDao
) {
    fun getTags(search: String?, category: Boolean?, categoryType: CategoryType?): List<Tag> {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        return tagDao.findByBalanceSheetOrderByName(balanceSheet)
            .filter { search == null || it.name.lowercase().startsWith(search) }
            .filter { category == null || category && (categoryType == null || it.categoryType == categoryType) }
    }

    fun update(tag: Tag) {
        tagDao.save(tag)
    }
}