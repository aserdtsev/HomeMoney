package ru.serdtsev.homemoney.port.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.domain.model.moneyoper.CategoryType
import ru.serdtsev.homemoney.domain.model.moneyoper.Tag
import ru.serdtsev.homemoney.domain.repository.TagRepository
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder

@Service
@Transactional
class TagService(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val tagRepository: TagRepository
) {
    fun getTags(search: String?, category: Boolean?, categoryType: CategoryType?): List<Tag> {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        return tagRepository.findByBalanceSheetOrderByName()
            .filter { search == null || it.name.lowercase().startsWith(search) }
            .filter { category == null || category && (categoryType == null || it.categoryType == categoryType) }
    }
}