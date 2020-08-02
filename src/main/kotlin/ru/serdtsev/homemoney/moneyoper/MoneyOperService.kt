package ru.serdtsev.homemoney.moneyoper

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.account.AccountRepository
import ru.serdtsev.homemoney.account.model.Balance
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
                .map { createMoneyOperByTemplate(balanceSheet, it) }
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

    private fun createMoneyOperByTemplate(balanceSheet: BalanceSheet?, recurrenceOper: RecurrenceOper): MoneyOper {
        val template = recurrenceOper.template
        val oper = MoneyOper(UUID.randomUUID(), balanceSheet!!, MoneyOperStatus.recurrence,
                recurrenceOper.nextDate, 0, template.labels, template.comment, template.period)
        oper.addItems(template.items)
        oper.fromAccId = template.fromAccId
        oper.toAccId = template.toAccId
        oper.recurrenceId = template.recurrenceId
        return oper
    }

    fun createRecurrenceOper(balanceSheet: BalanceSheet, operId: UUID) {
        val sample = moneyOperRepo.findByIdOrNull(operId)!!
        checkMoneyOperBelongsBalanceSheet(sample, balanceSheet.id)
        val template = newMoneyOper(balanceSheet, UUID.randomUUID(), MoneyOperStatus.template, sample.performed, null,
                sample.labels, sample.comment, sample.period, sample.fromAccId, sample.toAccId, sample.getAmount(),
                sample.getToAmount(), null, null)
        val recurrenceOper = RecurrenceOper(UUID.randomUUID(), balanceSheet, template, sample.performed)
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

    /**
     * Создает экземпляр MoneyOper.
     */
    fun newMoneyOper(balanceSheet: BalanceSheet, moneyOperId: UUID, status: MoneyOperStatus, performed: LocalDate,
            dateNum: Int?, labels: Collection<Label>, comment: String?, period: Period?, fromAccId: UUID, toAccId: UUID,
            amount: BigDecimal, toAmount: BigDecimal, parentId: UUID? = null, recurrenceId: UUID? = null): MoneyOper {
        val oper = MoneyOper(moneyOperId, balanceSheet, status, performed, dateNum, labels, comment, period)
        oper.recurrenceId = recurrenceId
        oper.fromAccId = fromAccId
        val fromAcc = accountRepo.findById(fromAccId).get()
        if (fromAcc is Balance) {
            oper.addItem(fromAcc, amount.negate(), performed)
        }
        oper.toAccId = toAccId
        val toAcc = accountRepo.findById(toAccId).get()
        if (toAcc is Balance) {
            oper.addItem(toAcc, toAmount, performed)
        }
        if (parentId != null) {
            val parentOper = moneyOperRepo.findById(parentId).get()
            oper.parentOper = parentOper
        }
        return oper
    }

    fun updateRecurrenceOper(balanceSheet: BalanceSheet, recurrenceOperDto: RecurrenceOperDto) {
        val recurrenceOper = recurrenceOperRepo.findByIdOrNull(recurrenceOperDto.id)!!
        recurrenceOper.nextDate = recurrenceOperDto.nextDate
        val template = recurrenceOper.template
        updateFromAccount(template, recurrenceOperDto.fromAccId)
        updateToAccount(template, recurrenceOperDto.toAccId)
        updateAmount(template, recurrenceOperDto.amount)
        updateToAmount(template, recurrenceOperDto.toAmount)
        template.comment = recurrenceOperDto.comment
        val labels: Collection<Label> = getLabelsByStrings(balanceSheet, recurrenceOperDto.labels)
        template.setLabels(labels)
        recurrenceOperRepo.save(recurrenceOper)
    }

    fun updateAmount(oper: MoneyOper, amount: BigDecimal) {
        if (oper.getAmount().compareTo(amount) == 0) return
        oper.items
                .filter { it.value.signum() < 0 }
                .forEach { it.value = amount.negate() }
    }

    fun updateToAmount(oper: MoneyOper, amount: BigDecimal?) {
        if (oper.getToAmount().compareTo(amount) == 0) return
        oper.items
                .filter { it.value.signum() > 0 }
                .forEach { it.value = amount!! }
    }

    fun updateFromAccount(oper: MoneyOper, accId: UUID) {
        if (oper.fromAccId == accId) return
        replaceBalance(oper, oper.fromAccId, accId)
        oper.fromAccId = accId
    }

    private fun replaceBalance(oper: MoneyOper, oldAccId: UUID, newAccId: UUID) {
        oper.items
                .filter { it.balance.id == oldAccId }
                .forEach {
                    val balance = accountRepo.findByIdOrNull(newAccId) as Balance
                    it.balance = balance
                }
    }

    fun updateToAccount(oper: MoneyOper, accId: UUID) {
        if (oper.toAccId == accId) return
        replaceBalance(oper, oper.toAccId, accId)
        oper.toAccId = accId
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
            MoneyOperDto(moneyOper.id, moneyOper.status, moneyOper.performed,
                    moneyOper.fromAccId, moneyOper.toAccId, moneyOper.getAmount().abs(), moneyOper.currencyCode,
                    moneyOper.getToAmount(), moneyOper.toCurrencyCode, moneyOper.period, moneyOper.comment,
                    getStringsByLabels(moneyOper.labels), moneyOper.dateNum, moneyOper.getParentOperId(),
                    moneyOper.recurrenceId, moneyOper.created).apply {
                fromAccName = getAccountName(moneyOper.fromAccId)
                toAccName = getAccountName(moneyOper.toAccId)
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