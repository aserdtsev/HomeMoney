package ru.serdtsev.homemoney.port.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.moneyoper.CategoryType
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperType
import ru.serdtsev.homemoney.domain.model.moneyoper.Tag
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.domain.repository.TagRepository
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class TagService(
    private val tagRepository: TagRepository,
    private val moneyOperRepository: MoneyOperRepository
) {
    fun getTags(bsId: UUID): List<Tag> = tagRepository.findByBalanceSheetOrderByName()

    fun getTags(search: String?, category: Boolean?, categoryType: CategoryType?): List<Tag> {
        return tagRepository.findByBalanceSheetOrderByName()
            .filter { search == null || it.name.lowercase().startsWith(search) }
            .filter { category == null || category && (categoryType == null || it.categoryType == categoryType) }
    }

    fun getSuggestTags(operType: String, search: String?, tags: List<String>): List<Tag> {
        return if (search.isNullOrEmpty()) {
            if (operType != MoneyOperType.transfer.name && tags.isEmpty()) {
                // Вернем только тэги-категории в зависимости от типа операции.
                tagRepository.findByBalanceSheetOrderByName()
                    .filter { !(it.arc) && it.isCategory && it.categoryType!!.name == operType }
            } else {
                // Найдем 10 наиболее часто используемых тегов-некатегорий за последние 30 дней.
                val startDate = LocalDate.now().minusDays(30)
                moneyOperRepository.findByBalanceSheetAndStatusAndPerformedGreaterThan(MoneyOperStatus.done, startDate)
                    .flatMap { it.tags }
                    .filter { !(it.arc) && !it.isCategory && !tags.contains(it.name) }
                    .groupingBy { it }.eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .map { it.key }
            }
        } else {
            tagRepository.findByBalanceSheetOrderByName()
                .filter { !(it.arc) && it.name.startsWith(search, true) }
        }
    }

    fun tagContains(tag: Tag, search: String): Boolean {
        return tag.name.lowercase().contains(search) ||
                tag.isCategory && tag.rootId?.let { tagRepository.findByIdOrNull(it) }
            ?.let { tagContains(it, search) } ?: false
    }

    fun getTagsByStrings(strTags: List<String>): MutableList<Tag> = strTags.map { findOrCreateTag(it) }.toMutableList()

    private fun findOrCreateTag(name: String): Tag =
        tagRepository.findOrNullByBalanceSheetAndName(name) ?: run {
            Tag(name).apply { DomainEventPublisher.instance.publish(this) }
        }

}