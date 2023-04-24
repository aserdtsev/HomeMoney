package ru.serdtsev.homemoney.moneyoper.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.account.dao.BalanceDao
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.BalanceSheetDao
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.dao.MoneyOperDao
import ru.serdtsev.homemoney.moneyoper.dao.RecurrenceOperDao
import ru.serdtsev.homemoney.moneyoper.dao.TagDao
import ru.serdtsev.homemoney.moneyoper.dto.RecurrenceOperDto
import ru.serdtsev.homemoney.moneyoper.model.*
import java.lang.UnsupportedOperationException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Предоставляет методы работы с денежными операциями
 */
@Service
class MoneyOperService (
    private val balanceSheetDao: BalanceSheetDao,
    private val moneyOperDao: MoneyOperDao,
    private val recurrenceOperDao: RecurrenceOperDao,
    private val balanceDao: BalanceDao,
    private val tagDao: TagDao,
) {
    fun createMoneyOper(moneyOper: MoneyOper, balanceSheet: BalanceSheet): List<MoneyOper> {
        val moneyOpers: MutableList<MoneyOper> = mutableListOf()
        save(moneyOper)
        moneyOpers.add(moneyOper)
        newReserveMoneyOper(balanceSheet, moneyOper)?.let { moneyOpers.add(it) }
        moneyOper.recurrenceId?.also { skipRecurrenceOper(balanceSheet, it) }
        if ((moneyOper.status == MoneyOperStatus.done || moneyOper.status == MoneyOperStatus.doneNew)
            && !moneyOper.performed.isAfter(LocalDate.now())) {
            moneyOpers.forEach { it.complete() }
        } else {
            moneyOpers.forEach { it.status = MoneyOperStatus.pending }
        }
        moneyOpers.forEach { save(it) }
        return moneyOpers
    }

    fun updateMoneyOper(newOper: MoneyOper, origOper: MoneyOper) {
        MoneyOper.merge(newOper, origOper).forEach { model ->
            when (model) {
                is MoneyOper -> moneyOperDao.save(model)
                is Balance -> balanceDao.save(model)
                else -> throw UnsupportedOperationException()
            }
        }
    }

    private fun newReserveMoneyOper(balanceSheet: BalanceSheet, oper: MoneyOper): MoneyOper? {
        return if (oper.items.any { it.balance.reserve != null }) {
            val tags = oper.tags
            val dateNum = oper.dateNum ?: 1
            val reserveMoneyOper = MoneyOper(balanceSheet, MoneyOperStatus.pending, oper.performed, dateNum, tags,
                oper.comment, oper.period)
            oper.items
                .filter { it.balance.reserve != null }
                .forEach { reserveMoneyOper.addItem(it.balance.reserve!!, it.value, it.performed, it.index + 1) }
            reserveMoneyOper
        }
        else null
    }

    fun save(moneyOper: MoneyOper) {
        moneyOperDao.save(moneyOper)
    }

    fun findRecurrenceOper(id: UUID): RecurrenceOper? {
        return recurrenceOperDao.findByIdOrNull(id)
    }

    fun save(recurrenceOper: RecurrenceOper) {
        recurrenceOperDao.save(recurrenceOper)
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
            recurrenceOperDao.findByBalanceSheetAndArc(balanceSheet, false).filter { isOperMatchSearch(it, search) }

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
        val sample = moneyOperDao.findById(operId)
        checkMoneyOperBelongsBalanceSheet(sample, balanceSheet.id)
        val template = MoneyOper(balanceSheet, MoneyOperStatus.template, sample.performed, 0, sample.tags,
                sample.comment, sample.period)
        sample.items.forEach { template.addItem(it.balance, it.value, it.performed) }
        val recurrenceOper = RecurrenceOper(balanceSheet, template, sample.performed)
        recurrenceOper.skipNextDate()
        moneyOperDao.save(template)
        recurrenceOperDao.save(recurrenceOper)

        // todo поправить эту косоту
        template.recurrenceId = recurrenceOper.id
        moneyOperDao.save(template)
        sample.recurrenceId = recurrenceOper.id
        moneyOperDao.save(sample)
    }

    fun deleteRecurrenceOper(balanceSheet: BalanceSheet, recurrenceId: UUID) {
        val recurrenceOper = recurrenceOperDao.findByIdOrNull(recurrenceId)!!
        recurrenceOper.arc()
        recurrenceOperDao.save(recurrenceOper)
        log.info("RecurrenceOper '{}' moved to archive.", recurrenceId)
    }

    fun skipRecurrenceOper(balanceSheet: BalanceSheet, recurrenceId: UUID) {
        val recurrenceOper = recurrenceOperDao.findByIdOrNull(recurrenceId)!!
        recurrenceOper.skipNextDate()
        recurrenceOperDao.save(recurrenceOper)
    }

    fun updateRecurrenceOper(balanceSheet: BalanceSheet, recurrenceOperDto: RecurrenceOperDto) {
        val origRecurrenceOper = recurrenceOperDao.findByIdOrNull(recurrenceOperDto.id)!!
        origRecurrenceOper.nextDate = recurrenceOperDto.nextDate
        val origTemplate = origRecurrenceOper.template
        recurrenceOperDto.items.forEach { item ->
            val origItem = origTemplate.items.firstOrNull { origItem -> origItem.id == item.id }
            if (origItem != null) with (origItem) {
                balance = balanceDao.findById(item.balanceId)
                value = item.value.multiply(item.sgn.toBigDecimal())
                index = item.index
            } else {
                val balance = balanceDao.findById(item.balanceId)
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
        recurrenceOperDao.save(origRecurrenceOper)
    }

    fun checkMoneyOperBelongsBalanceSheet(oper: MoneyOper, bsId: UUID) =
            assert(oper.balanceSheet.id == bsId) { "MoneyOper id='${oper.id}' belongs the other balance sheet." }

    fun getTagsByStrings(balanceSheet: BalanceSheet, strTags: List<String>): MutableList<Tag> =
            strTags.map { findOrCreateTag(balanceSheet, it) }.toMutableList()

    fun findOrCreateTag(balanceSheet: BalanceSheet, name: String): Tag =
            tagDao.findByBalanceSheetAndName(balanceSheet, name) ?: run { createSimpleTag(balanceSheet, name) }

    private fun createSimpleTag(balanceSheet: BalanceSheet, name: String): Tag =
            Tag(UUID.randomUUID(), balanceSheet, name).apply { tagDao.save(this) }

    fun getSuggestTags(bsId: UUID, operType: String, search: String?, tags: List<String>): List<Tag> {
        val balanceSheet = balanceSheetDao.findById(bsId)
        return if (search.isNullOrEmpty()) {
            if (operType != MoneyOperType.transfer.name && tags.isEmpty()) {
                // Вернем только тэги-категории в зависимости от типа операции.
                tagDao.findByBalanceSheetOrderByName(balanceSheet)
                        .filter { !(it.arc) && it.isCategory && it.categoryType!!.name == operType }
            } else {
                // Найдем 10 наиболее часто используемых тегов-некатегорий за последние 30 дней.
                val startDate = LocalDate.now().minusDays(30)
                moneyOperDao.findByBalanceSheetAndStatusAndPerformedGreaterThan(balanceSheet, MoneyOperStatus.done, startDate)
                        .flatMap { it.tags }
                        .filter { !(it.arc) && !it.isCategory && !tags.contains(it.name) }
                        .groupingBy { it }.eachCount()
                        .entries
                        .sortedByDescending { it.value }
                        .map { it.key }
            }
        } else {
            tagDao.findByBalanceSheetOrderByName(balanceSheet)
                    .filter { !(it.arc) && it.name.startsWith(search, true) }
        }
    }

    fun getTags(bsId: UUID): List<Tag> {
        val balanceSheet = balanceSheetDao.findById(bsId)
        return tagDao.findByBalanceSheetOrderByName(balanceSheet)
    }

    companion object {
        private val log = KotlinLogging.logger {  }
        private val SearchDateRegex = "\\d{4}-\\d{2}-\\d{2}".toRegex()
        private val SearchUuidRegex = "\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}".toRegex()
        private val SearchMoneyRegex = "\\d+\\.*\\d*".toRegex()
    }
}