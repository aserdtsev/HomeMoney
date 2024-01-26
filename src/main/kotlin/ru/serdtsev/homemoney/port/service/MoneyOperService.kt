package ru.serdtsev.homemoney.port.service

import org.springframework.data.domain.*
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.domain.repository.RecurrenceOperRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import java.util.function.Consumer
import java.util.function.Function

/**
 * Предоставляет методы работы с денежными операциями
 */
@Service
class MoneyOperService(
    private val moneyOperRepository: MoneyOperRepository,
    private val tagService: TagService,
    private val recurrenceOperRepository: RecurrenceOperRepository
    ) {
    fun getMoneyOpers(status: MoneyOperStatus, search: String, limit: Int, offset: Int = 0): List<MoneyOper> {
        val sort = Sort.by(Sort.Direction.DESC, "trn_date")
            .and(Sort.by(Sort.Direction.DESC,"date_num"))
            .and(Sort.by(Sort.Direction.DESC, "created_ts"))
        return if (search.isBlank())
            getMoneyOpers(status, sort, limit, offset)
        else
            getMoneyOpersBySearch(status, search.lowercase(Locale.getDefault()), sort, limit, offset)
    }

    fun getUpcomingMoneyOpers(search: String): List<MoneyOper> {
        return getMoneyOpers(MoneyOperStatus.Trend, search, Int.MAX_VALUE)
            .map { trendMoneyOper -> MoneyOper(trendMoneyOper.status, trendMoneyOper.performed, trendMoneyOper.tags,
                trendMoneyOper.comment, Period.Month, trendMoneyOper.recurrenceParams)
                .apply { trendMoneyOper.items.forEach {
                    addItem(it.balance, it.value, it.performed, it.index)
                } }
            }
    }

    private fun getMoneyOpers(status: MoneyOperStatus, sort: Sort, limit: Int, offset: Int): List<MoneyOper> {
        val pageRequest: Pageable = PageRequest.of(offset / (limit - 1), limit - 1, sort)
        return moneyOperRepository.findByStatus(status, pageRequest).content.plus(
            moneyOperRepository.findByStatus(status, pageRequest.next()).content.take(1)
        )
    }

    private fun getMoneyOpersBySearch(status: MoneyOperStatus, search: String, sort: Sort,
        limit: Int, offset: Int): List<MoneyOper> {
        var pageRequest: Pageable = PageRequest.of(0, 100, sort)
        val opers = ArrayList<MoneyOper>()
        var page: Page<*>
        val pager: Function<Pageable, Page<*>>
        val adder: Consumer<Page<*>>
        @Suppress("UNCHECKED_CAST")
        when {
            search.matches(SearchDateRegex) -> {
                // по дате в формате ISO
                pager = Function { pageable: Pageable ->
                    moneyOperRepository.findByStatusAndPerformed(status,
                        LocalDate.parse(search),
                        pageable)
                }
                adder = Consumer { p: Page<*> -> opers.addAll((p as Page<MoneyOper>).content) }
            }
            search.matches(SearchUuidRegex) -> {
                // по идентификатору операции
                pager = Function { pageable: Pageable ->
                    val list = (moneyOperRepository.findByIdOrNull(UUID.fromString(search))
                        ?.let { listOf(it) }
                        ?.filter { it.status == status }
                        ?: listOf())
                        .toMutableList()
                    PageImpl(list, pageable, list.size.toLong())
                }
                adder = Consumer { p: Page<*> -> opers.addAll((p as Page<MoneyOper>).content) }
            }
            search.matches(SearchMoneyRegex) -> {
                // по сумме операции
                pageRequest = PageRequest.of(pageRequest.pageNumber, pageRequest.pageSize)
                pager = Function { pageable: Pageable ->
                    val value = BigDecimal(search).abs()
                    moneyOperRepository.findByValueOrderByPerformedDesc(value, pageable)
                }
                adder = Consumer { p: Page<*> ->
                    (p as Page<MoneyOper>).content
                        .filter { it.status !== status }
                        .forEach { e: MoneyOper -> opers.add(e) }
                }
            }
            else -> {
                pager = Function { pageable: Pageable? -> moneyOperRepository.findByStatus(status, pageable!!) }
                adder = Consumer { p: Page<*> ->
                    val opersChunk = (p as Page<MoneyOper>).content
                        .filter { oper: MoneyOper ->
                            (oper.items.any { item: MoneyOperItem -> item.balance.name.lowercase(Locale.getDefault())
                                .contains(search) } // по комментарию
                                    || oper.comment?.lowercase(Locale.getDefault())?.contains(search) ?: false // по меткам
                                    || oper.tags.any { tagService.tagContains(it, search) })
                        }
                    opers.addAll(opersChunk)
                }
            }
        }
        do {
            page = pager.apply(pageRequest)
            adder.accept(page)
            pageRequest = pageRequest.next()
        } while (opers.size - offset < limit && page.hasNext())
        return opers.subList(offset, opers.size).take(limit)
    }

    /**
     * Возвращает следующие повторы операций.
     */
    fun getNextRecurrenceOpers(search: String, beforeDate: LocalDate?): List<MoneyOper> {
        return getRecurrenceOpers(search)
                .filter { it.nextDate.isBefore(beforeDate) }
                .map { it.createNextMoneyOper() }
    }

    /**
     * Возвращает повторяющиеся операции.
     */
    fun getRecurrenceOpers(search: String): List<RecurrenceOper> =
            recurrenceOperRepository.findByBalanceSheetAndArc(false).filter { isOperMatchSearch(it, search) }

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

    companion object {
        private val SearchDateRegex = "\\d{4}-\\d{2}-\\d{2}".toRegex()
        private val SearchUuidRegex = "\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}".toRegex()
        private val SearchMoneyRegex = "\\d+\\.*\\d*".toRegex()
    }
}