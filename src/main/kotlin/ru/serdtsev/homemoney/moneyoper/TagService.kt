package ru.serdtsev.homemoney.moneyoper

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository
import ru.serdtsev.homemoney.moneyoper.model.CategoryType
import ru.serdtsev.homemoney.moneyoper.model.Tag
import java.util.*

@Service
@Transactional
class TagService(val balanceSheetRepo: BalanceSheetRepository, val tagRepo: TagRepository) {
    fun getTags(bsId: UUID, search: String?, category: Boolean?, categoryType: CategoryType?): List<Tag> {
        val balanceSheet = balanceSheetRepo.findByIdOrNull(bsId)!!
        return tagRepo.findByBalanceSheetOrderByName(balanceSheet)
            .filter { search == null || it.name.toLowerCase().startsWith(search) }
            .filter { category == null || category && (categoryType == null || it.categoryType == categoryType) }
    }

    fun updateTag(tag: Tag) {
        tagRepo.save(tag)
    }
}