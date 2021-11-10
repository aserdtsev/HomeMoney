package ru.serdtsev.homemoney.moneyoper.service

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.account.AccountRepo
import ru.serdtsev.homemoney.account.BalanceRepo
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.balancesheet.dao.BalanceSheetRepo
import ru.serdtsev.homemoney.moneyoper.dao.MoneyOperRepo
import ru.serdtsev.homemoney.moneyoper.dao.RecurrenceOperRepo
import ru.serdtsev.homemoney.moneyoper.dao.TagRepo
import ru.serdtsev.homemoney.moneyoper.dto.MoneyOperDto
import ru.serdtsev.homemoney.moneyoper.dto.RecurrenceOperDto
import ru.serdtsev.homemoney.moneyoper.model.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Предоставляет методы работы с денежными операциями
 */
@Service
class MoneyOperService @Autowired constructor(
    private val balanceSheetRepo: BalanceSheetRepo,
    private val moneyOperRepo: MoneyOperRepo,
    private val recurrenceOperRepo: RecurrenceOperRepo,
    private val accountRepo: AccountRepo,
    private val balanceRepo: BalanceRepo,
    private val tagRepo: TagRepo
) {
    fun save(moneyOper: MoneyOper) {
        moneyOperRepo.save(moneyOper)
    }

    fun findRecurrenceOper(id: UUID): RecurrenceOper? {
        return recurrenceOperRepo.findByIdOrNull(id)
    }

    fun save(recurrenceOper: RecurrenceOper) {
        recurrenceOperRepo.save(recurrenceOper)
    }

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
            recurrenceOperRepo.findByBalanceSheet(balanceSheet).filter { !it.arc && isOperMatchSearch(it, search) }

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
        val sample = moneyOperRepo.findByIdOrNull(operId)!!
        checkMoneyOperBelongsBalanceSheet(sample, balanceSheet.id)
        val template = MoneyOper(balanceSheet, MoneyOperStatus.template, sample.performed, 0, sample.tags,
                sample.comment, sample.period)
        sample.items.forEach { template.addItem(it.balance, it.value, it.performed) }
        val recurrenceOper = RecurrenceOper(balanceSheet, template, sample.performed)
        recurrenceOper.skipNextDate()
        moneyOperRepo.save(template)
        recurrenceOperRepo.save(recurrenceOper)

        // todo поправить эту косоту
        template.recurrenceId = recurrenceOper.id
        moneyOperRepo.save(template)
        sample.recurrenceId = recurrenceOper.id
        moneyOperRepo.save(sample)
    }

    fun deleteRecurrenceOper(balanceSheet: BalanceSheet, recurrenceId: UUID) {
        val recurrenceOper = recurrenceOperRepo.findByIdOrNull(recurrenceId)!!
        recurrenceOper.arc()
        recurrenceOperRepo.save(recurrenceOper)
        log.info("RecurrenceOper '{}' moved to archive.", recurrenceId)
    }

    fun skipRecurrenceOper(balanceSheet: BalanceSheet, recurrenceId: UUID) {
        val recurrenceOper = recurrenceOperRepo.findByIdOrNull(recurrenceId)!!
        recurrenceOper.skipNextDate()
        recurrenceOperRepo.save(recurrenceOper)
    }

    fun updateRecurrenceOper(balanceSheet: BalanceSheet, recurrenceOperDto: RecurrenceOperDto) {
        val origRecurrenceOper = recurrenceOperRepo.findByIdOrNull(recurrenceOperDto.id)!!
        origRecurrenceOper.nextDate = recurrenceOperDto.nextDate
        val origTemplate = origRecurrenceOper.template
        recurrenceOperDto.items.forEach { item ->
            val origItem = origTemplate.items.firstOrNull { origItem -> origItem.id == item.id }
            if (origItem != null) with (origItem) {
                balance = balanceRepo.findByIdOrNull(item.balanceId)!!
                value = item.value.multiply(item.sgn.toBigDecimal())
                index = item.index
            } else {
                val balance = balanceRepo.findByIdOrNull(item.balanceId)!!
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
        recurrenceOperRepo.save(origRecurrenceOper)
    }

    fun moneyOperDtoToMoneyOper(balanceSheet: BalanceSheet, moneyOperDto: MoneyOperDto): MoneyOper {
        val dateNum = moneyOperDto.dateNum ?: 0
        val tags = getTagsByStrings(balanceSheet, moneyOperDto.tags)
        val period = moneyOperDto.period ?: Period.month
        val oper = MoneyOper(moneyOperDto.id, balanceSheet, MoneyOperStatus.pending, moneyOperDto.operDate, dateNum, tags,
                moneyOperDto.comment, period)
        oper.recurrenceId = moneyOperDto.recurrenceId
        moneyOperDto.items.forEach {
            val balance = balanceRepo.findByIdOrNull(it.balanceId)!!
            val value = it.value.multiply(it.sgn.toBigDecimal())
            oper.addItem(balance, value, it.performedAt, it.index, it.id)
        }
        return oper
    }

    fun updateMoneyOper(toOper: MoneyOper, fromOper: MoneyOper) {
        fromOper.items.forEach { item ->
            val origItem = toOper.items.firstOrNull { origItem -> origItem.id == item.id }
            if (origItem != null) with (origItem) {
                performed = item.performed
                balance = item.balance
                value = item.value
                index = item.index
            } else {
                toOper.addItem(item.balance, item.value, item.performed, item.index)
            }
        }
        toOper.items.retainAll(fromOper.items)
        toOper.performed = fromOper.performed
        toOper.setTags(fromOper.tags)
        toOper.dateNum = fromOper.dateNum
        toOper.period = fromOper.period
        toOper.comment = fromOper.comment
    }

    fun checkMoneyOperBelongsBalanceSheet(oper: MoneyOper, bsId: UUID) =
            assert(oper.balanceSheet.id == bsId) { "MoneyOper id='${oper.id}' belongs the other balance sheet." }

    fun getTagsByStrings(balanceSheet: BalanceSheet, strTags: List<String>): MutableList<Tag> =
            strTags.map { findOrCreateTag(balanceSheet, it) }.toMutableList()

    fun findOrCreateTag(balanceSheet: BalanceSheet, name: String): Tag =
            tagRepo.findByBalanceSheetAndName(balanceSheet, name) ?: run { createSimpleTag(balanceSheet, name) }

    private fun createSimpleTag(balanceSheet: BalanceSheet, name: String): Tag =
            Tag(UUID.randomUUID(), balanceSheet, name).apply { tagRepo.save(this) }

    fun getSuggestTags(bsId: UUID, operType: String, search: String?, tags: List<String>): List<Tag> {
        val balanceSheet = balanceSheetRepo.findById(bsId).get()
        return if (search.isNullOrEmpty()) {
            if (operType != MoneyOperType.transfer.name && tags.isEmpty()) {
                // Вернем только тэги-категории в зависимости от типа операции.
                tagRepo.findByBalanceSheetOrderByName(balanceSheet)
                        .filter { !(it.arc ?: false) && it.isCategory!! && it.categoryType!!.name == operType }
            } else {
                // Найдем 10 наиболее часто используемых тегов-некатегорий за последние 30 дней.
                val startDate = LocalDate.now().minusDays(30)
                moneyOperRepo.findByBalanceSheetAndStatusAndPerformedGreaterThan(balanceSheet, MoneyOperStatus.done, startDate)
                        .flatMap { it.tags }
                        .filter { !(it.arc ?: false) && !it.isCategory!! && !tags.contains(it.name) }
                        .groupingBy { it }.eachCount()
                        .entries
                        .sortedByDescending { it.value }
                        .map { it.key }
            }
        } else {
            tagRepo.findByBalanceSheetOrderByName(balanceSheet)
                    .filter { !(it.arc ?: false) && it.name.startsWith(search, true) }
        }
    }

    fun getAccountName(accountId: UUID): String {
        val account = accountRepo.findByIdOrNull(accountId)!!
        return account.name
    }

    fun getTags(bsId: UUID): List<Tag> {
        val balanceSheet = balanceSheetRepo.findByIdOrNull(bsId)!!
        return tagRepo.findByBalanceSheetOrderByName(balanceSheet)
    }

    companion object {
        private val log = KotlinLogging.logger {  }
        private val SearchDateRegex = "\\p{Digit}{4}-\\p{Digit}{2}-\\p{Digit}{2}".toRegex()
        private val SearchUuidRegex = "\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}".toRegex()
        private val SearchMoneyRegex = "\\p{Digit}+\\.*\\p{Digit}*".toRegex()
    }
}