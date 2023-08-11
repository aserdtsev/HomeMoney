package ru.serdtsev.homemoney.port.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.domain.repository.RecurrenceOperRepository
import ru.serdtsev.homemoney.domain.repository.TagRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Предоставляет методы работы с денежными операциями
 */
@Service
class MoneyOperService(
    private val moneyOperRepository: MoneyOperRepository,
    private val recurrenceOperRepository: RecurrenceOperRepository,
    private val tagRepository: TagRepository,
) {
    /**
     * Возвращает следующие повторы операций.
     */
    fun getNextRecurrenceOpers(balanceSheet: BalanceSheet, search: String, beforeDate: LocalDate?): List<MoneyOper> {
        return getRecurrenceOpers(balanceSheet, search)
                .filter { it.nextDate.isBefore(beforeDate) }
                .map { it.createNextMoneyOper() }
    }

    /**
     * Возвращает повторяющиеся операции.
     */
    fun getRecurrenceOpers(balanceSheet: BalanceSheet, search: String): List<RecurrenceOper> =
            recurrenceOperRepository.findByBalanceSheetAndArc(balanceSheet, false).filter { isOperMatchSearch(it, search) }

    /**
     * @return true, если шаблон операции соответствует строке поиска
     */
    private fun isOperMatchSearch(recurrenceOper: RecurrenceOper, search: String): Boolean {
        val template = recurrenceOper.template
        return when {
            search.isBlank() -> true
            search.matches(searchDateRegex) -> {
                // по дате в формате ISO
                recurrenceOper.nextDate.isEqual(LocalDate.parse(search))
            }
            search.matches(searchUuidRegex) -> {
                // по идентификатору операции
                template.id == UUID.fromString(search)
            }
            search.matches(searchMoneyRegex) -> {
                // по сумме операции
                template.items.any { it.value.plus().compareTo(BigDecimal(search)) == 0 }
            }
            else -> {
                (template.items.any { it.balance.name.lowercase().contains(search) }
                        || template.tags.any { it.name.lowercase().contains(search) })
                        || template.comment?.lowercase()?.contains(search) ?: false
            }
        }
    }

    fun getTagsByStrings(strTags: List<String>): MutableList<Tag> = strTags.map { findOrCreateTag(it) }.toMutableList()

    fun findOrCreateTag(name: String): Tag =
            tagRepository.findByBalanceSheetAndName(name) ?: run { createSimpleTag(name) }

    private fun createSimpleTag(name: String): Tag =
            Tag(name).apply {
                DomainEventPublisher.instance.publish(this)
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

    fun getTags(bsId: UUID): List<Tag> = tagRepository.findByBalanceSheetOrderByName()

    companion object {
        private val log = KotlinLogging.logger {  }
        private val searchDateRegex = "\\d{4}-\\d{2}-\\d{2}".toRegex()
        private val searchUuidRegex = "\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}".toRegex()
        private val searchMoneyRegex = "\\d+\\.*\\d*".toRegex()
    }
}