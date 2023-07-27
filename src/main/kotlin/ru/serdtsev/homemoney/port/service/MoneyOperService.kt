package ru.serdtsev.homemoney.port.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.domain.repository.*
import ru.serdtsev.homemoney.infra.dao.TagDao
import ru.serdtsev.homemoney.port.dto.moneyoper.RecurrenceOperDto
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Предоставляет методы работы с денежными операциями
 */
@Service
class MoneyOperService (
    private val balanceSheetRepository: BalanceSheetRepository,
    private val moneyOperRepository: MoneyOperRepository,
    private val recurrenceOperRepository: RecurrenceOperRepository,
    private val balanceRepository: BalanceRepository,
    private val tagRepository: TagRepository,
) {
    // todo Порефакторить
    fun save(recurrenceOper: RecurrenceOper) = DomainEventPublisher.instance.publish(recurrenceOper)

    /**
     * Возвращает следующие повторы операций.
     */
    fun getNextRecurrenceOpers(balanceSheet: BalanceSheet, search: String, beforeDate: LocalDate?): List<MoneyOper> {
        return getRecurrenceOpers(balanceSheet, search)
                .filter { it.nextDate.isBefore(beforeDate) }
                .map { createMoneyOperByRecurrenceOper(balanceSheet, it) }
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
            search.matches(SearchDateRegex) -> {
                // по дате в формате ISO
                recurrenceOper.nextDate.isEqual(LocalDate.parse(search))
            }
            search.matches(SearchUuidRegex) -> {
                // по идентификатору операции
                template.id == UUID.fromString(search)
            }
            search.matches(SearchMoneyRegex) -> {
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

    private fun createMoneyOperByRecurrenceOper(balanceSheet: BalanceSheet, recurrenceOper: RecurrenceOper): MoneyOper {
        val template = recurrenceOper.template
        val performed = recurrenceOper.nextDate
        val oper = MoneyOper(balanceSheet, MoneyOperStatus.recurrence, performed, 0, template.tags,
                template.comment, template.period)
        template.items.forEach { oper.addItem(it.balance, it.value, performed) }
        oper.recurrenceId = template.recurrenceId
        return oper
    }

    fun createRecurrenceOper(balanceSheet: BalanceSheet, operId: UUID) {
        val sample = moneyOperRepository.findById(operId)
        checkMoneyOperBelongsBalanceSheet(sample, balanceSheet.id)
        val template = MoneyOper(balanceSheet, MoneyOperStatus.template, sample.performed, 0, sample.tags,
                sample.comment, sample.period)
        sample.items.forEach { template.addItem(it.balance, it.value, it.performed) }
        val recurrenceOper = RecurrenceOper(balanceSheet, template, sample.performed)
        recurrenceOper.skipNextDate()
        DomainEventPublisher.instance.publish(template)
        DomainEventPublisher.instance.publish(recurrenceOper)

        // todo поправить эту косоту
        template.recurrenceId = recurrenceOper.id
        DomainEventPublisher.instance.publish(template)
        sample.recurrenceId = recurrenceOper.id
        DomainEventPublisher.instance.publish(sample)
    }

    fun deleteRecurrenceOper(balanceSheet: BalanceSheet, recurrenceId: UUID) {
        val recurrenceOper = recurrenceOperRepository.findByIdOrNull(recurrenceId)!!
        recurrenceOper.arc()
        DomainEventPublisher.instance.publish(recurrenceOper)
        log.info("RecurrenceOper '{}' moved to archive.", recurrenceId)
    }

    fun skipRecurrenceOper(balanceSheet: BalanceSheet, recurrenceId: UUID) {
        val recurrenceOper = recurrenceOperRepository.findByIdOrNull(recurrenceId)!!
        recurrenceOper.skipNextDate()
        DomainEventPublisher.instance.publish(recurrenceOper)
    }

    fun updateRecurrenceOper(balanceSheet: BalanceSheet, recurrenceOperDto: RecurrenceOperDto) {
        val origRecurrenceOper = recurrenceOperRepository.findByIdOrNull(recurrenceOperDto.id)!!
        origRecurrenceOper.nextDate = recurrenceOperDto.nextDate
        val origTemplate = origRecurrenceOper.template
        recurrenceOperDto.items.forEach { item ->
            val origItem = origTemplate.items.firstOrNull { origItem -> origItem.id == item.id }
            if (origItem != null) with (origItem) {
                balance = balanceRepository.findById(item.balanceId)
                value = item.value.multiply(item.sgn.toBigDecimal())
                index = item.index
            } else {
                val balance = balanceRepository.findById(item.balanceId)
                val value = item.value.multiply(item.sgn.toBigDecimal())
                origTemplate.addItem(balance, value, index = item.index)
            }
        }
        origTemplate.items.removeIf {
            origTemplateItem -> !recurrenceOperDto.items.any { it.id == origTemplateItem.id }
        }
        origTemplate.comment = recurrenceOperDto.comment
        val tags: Collection<Tag> = getTagsByStrings(balanceSheet, recurrenceOperDto.tags)
        origTemplate.setTags(tags)
        DomainEventPublisher.instance.publish(origRecurrenceOper)
    }

    fun checkMoneyOperBelongsBalanceSheet(oper: MoneyOper, bsId: UUID) =
            assert(oper.balanceSheet.id == bsId) { "MoneyOper id='${oper.id}' belongs the other balance sheet." }

    fun getTagsByStrings(balanceSheet: BalanceSheet, strTags: List<String>): MutableList<Tag> =
            strTags.map { findOrCreateTag(balanceSheet, it) }.toMutableList()

    fun findOrCreateTag(balanceSheet: BalanceSheet, name: String): Tag =
            tagRepository.findByBalanceSheetAndName(balanceSheet, name) ?: run { createSimpleTag(balanceSheet, name) }

    private fun createSimpleTag(balanceSheet: BalanceSheet, name: String): Tag =
            Tag(UUID.randomUUID(), balanceSheet, name).apply {
                DomainEventPublisher.instance.publish(this)
            }

    fun getSuggestTags(bsId: UUID, operType: String, search: String?, tags: List<String>): List<Tag> {
        val balanceSheet = balanceSheetRepository.findById(bsId)
        return if (search.isNullOrEmpty()) {
            if (operType != MoneyOperType.transfer.name && tags.isEmpty()) {
                // Вернем только тэги-категории в зависимости от типа операции.
                tagRepository.findByBalanceSheetOrderByName(balanceSheet)
                        .filter { !(it.arc) && it.isCategory && it.categoryType!!.name == operType }
            } else {
                // Найдем 10 наиболее часто используемых тегов-некатегорий за последние 30 дней.
                val startDate = LocalDate.now().minusDays(30)
                moneyOperRepository.findByBalanceSheetAndStatusAndPerformedGreaterThan(balanceSheet, MoneyOperStatus.done, startDate)
                        .flatMap { it.tags }
                        .filter { !(it.arc) && !it.isCategory && !tags.contains(it.name) }
                        .groupingBy { it }.eachCount()
                        .entries
                        .sortedByDescending { it.value }
                        .map { it.key }
            }
        } else {
            tagRepository.findByBalanceSheetOrderByName(balanceSheet)
                    .filter { !(it.arc) && it.name.startsWith(search, true) }
        }
    }

    fun getTags(bsId: UUID): List<Tag> {
        val balanceSheet = balanceSheetRepository.findById(bsId)
        return tagRepository.findByBalanceSheetOrderByName(balanceSheet)
    }

    companion object {
        private val log = KotlinLogging.logger {  }
        private val SearchDateRegex = "\\d{4}-\\d{2}-\\d{2}".toRegex()
        private val SearchUuidRegex = "\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}".toRegex()
        private val SearchMoneyRegex = "\\d+\\.*\\d*".toRegex()
    }
}