package ru.serdtsev.homemoney.moneyoper

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.account.AccountRepository
import ru.serdtsev.homemoney.account.BalanceRepository
import ru.serdtsev.homemoney.account.model.Reserve
import ru.serdtsev.homemoney.account.model.ServiceAccount
import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository
import ru.serdtsev.homemoney.moneyoper.model.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Предоставляет методы работы с денежными операциями
 */
@Service
class MoneyOperService @Autowired constructor(
        @Qualifier("conversionService") private val conversionService: ConversionService,
        private val balanceSheetRepo: BalanceSheetRepository,
        private val moneyOperRepo: MoneyOperRepo,
        private val recurrenceOperRepo: RecurrenceOperRepo,
        private val accountRepo: AccountRepository,
        private val balanceRepo: BalanceRepository,
        private val labelRepo: LabelRepository
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
                (template.items.any { it.balance.name.toLowerCase().contains(search) }
                        || template.labels.any { it.name.toLowerCase().contains(search) })
                        || template.comment?.toLowerCase()?.contains(search) ?: false
            }
        }
    }

    private fun createMoneyOperByRecurrenceOper(balanceSheet: BalanceSheet, recurrenceOper: RecurrenceOper): MoneyOper {
        val template = recurrenceOper.template
        val performed = recurrenceOper.nextDate
        val oper = MoneyOper(balanceSheet, MoneyOperStatus.recurrence, performed, 0, template.labels,
                template.comment, template.period)
        template.items.forEach { oper.addItem(it.balance, it.value, performed) }
        oper.recurrenceId = template.recurrenceId
        return oper
    }

    fun createRecurrenceOper(balanceSheet: BalanceSheet, operId: UUID) {
        val sample = moneyOperRepo.findByIdOrNull(operId)!!
        checkMoneyOperBelongsBalanceSheet(sample, balanceSheet.id)
        val template = MoneyOper(balanceSheet, MoneyOperStatus.template, sample.performed, 0, sample.labels,
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
                value = item.value
                index = item.index
            } else {
                val balance = balanceRepo.findByIdOrNull(item.balanceId)!!
                origTemplate.addItem(balance, item.value, index = item.index)
            }
        }
        origTemplate.items.removeIf {
            origTemplateItem -> !recurrenceOperDto.items.any { it.id == origTemplateItem.id }
        }
        origTemplate.comment = recurrenceOperDto.comment
        val labels: Collection<Label> = getLabelsByStrings(balanceSheet, recurrenceOperDto.labels)
        origTemplate.setLabels(labels)
        recurrenceOperRepo.save(origRecurrenceOper)
    }

    fun moneyOperDtoToMoneyOper(balanceSheet: BalanceSheet, moneyOperDto: MoneyOperDto): MoneyOper {
        val dateNum = moneyOperDto.dateNum ?: 0
        val labels = getLabelsByStrings(balanceSheet, moneyOperDto.labels)
        val period = moneyOperDto.period ?: Period.month
        val oper = MoneyOper(balanceSheet, MoneyOperStatus.pending, moneyOperDto.operDate, dateNum, labels,
                moneyOperDto.comment, period)
        moneyOperDto.items.forEach {
            val balance = balanceRepo.findByIdOrNull(it.balanceId)!!
            oper.addItem(balance, it.value, it.performedAt, it.index)
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
        toOper.setLabels(fromOper.labels)
        toOper.dateNum = fromOper.dateNum
        toOper.period = fromOper.period
        toOper.comment = fromOper.comment
    }

    fun checkMoneyOperBelongsBalanceSheet(oper: MoneyOper, bsId: UUID) =
            assert(oper.balanceSheet.id == bsId) { "MoneyOper id='${oper.id}' belongs the other balance sheet." }

    fun getLabelsByStrings(balanceSheet: BalanceSheet, strLabels: List<String>): MutableList<Label> =
            strLabels.map { findOrCreateLabel(balanceSheet, it) }.toMutableList()

    fun findOrCreateLabel(balanceSheet: BalanceSheet, name: String): Label =
            labelRepo.findByBalanceSheetAndName(balanceSheet, name) ?: run { createSimpleLabel(balanceSheet, name) }

    private fun createSimpleLabel(balanceSheet: BalanceSheet, name: String): Label =
            Label(UUID.randomUUID(), balanceSheet, name).apply { labelRepo.save(this) }

    fun getStringsByLabels(labels: Collection<Label>): List<String> = labels.map { it.name }

    fun getSuggestLabels(bsId: UUID, moneyOper: MoneyOperDto): List<Label> {
        // Найдем 10 наиболее часто используемых меток-категорий за последние 30 дней.
        val startDate = LocalDate.now().minusDays(30)
        val balanceSheet = balanceSheetRepo.findById(bsId).get()
        return moneyOperRepo.findByBalanceSheetAndStatusAndPerformedGreaterThan(balanceSheet, MoneyOperStatus.done, startDate)
                .flatMap { it.labels }
                .filter { !moneyOper.labels.contains(it.name) }
                .groupingBy { it }.eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { it.key }
    }

    fun moneyOperToDto(moneyOper: MoneyOper): MoneyOperDto =
            MoneyOperDto(moneyOper.id, moneyOper.status, moneyOper.performed, moneyOper.period, moneyOper.comment,
                    getStringsByLabels(moneyOper.labels), moneyOper.dateNum, moneyOper.getParentOperId(),
                    moneyOper.recurrenceId, moneyOper.created).apply {
                if (moneyOper.type == MoneyOperType.transfer && moneyOper.items.any { it.balance is Reserve }) {
                    val operItem = moneyOper.items.first { it.balance is Reserve }
                    type = if (operItem.value.signum() > 0) MoneyOperType.income.name else MoneyOperType.expense.name
                } else
                    type = moneyOper.type.name
                items = moneyOper.items
                        .map { conversionService.convert(it, MoneyOperItemDto::class.java)!! }
                        .sortedBy { it.value }
            }

    fun getAccountName(accountId: UUID): String {
        val account = accountRepo.findByIdOrNull(accountId)!!
        return account.name
    }

    fun getLabels(bsId: UUID): List<Label> {
        val balanceSheet = balanceSheetRepo.findByIdOrNull(bsId)!!
        return labelRepo.findByBalanceSheetOrderByName(balanceSheet)
    }

    companion object {
        private val log = KotlinLogging.logger {  }
        private val SearchDateRegex = "\\p{Digit}{4}-\\p{Digit}{2}-\\p{Digit}{2}".toRegex()
        private val SearchUuidRegex = "\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}".toRegex()
        private val SearchMoneyRegex = "\\p{Digit}+\\.*\\p{Digit}*".toRegex()
    }
}