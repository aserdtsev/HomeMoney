package ru.serdtsev.homemoney.moneyoper

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.common.HmException
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.common.PagedList
import ru.serdtsev.homemoney.moneyoper.dao.MoneyOperItemRepo
import ru.serdtsev.homemoney.moneyoper.dao.MoneyOperRepo
import ru.serdtsev.homemoney.moneyoper.dao.TagRepo
import ru.serdtsev.homemoney.moneyoper.dto.MoneyOperDto
import ru.serdtsev.homemoney.moneyoper.model.*
import ru.serdtsev.homemoney.moneyoper.service.MoneyOperService
import java.math.BigDecimal
import java.sql.SQLException
import java.time.LocalDate
import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.IntStream

@RestController
@RequestMapping("/api/money-opers")
@Transactional
class MoneyOperController(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val moneyOperService: MoneyOperService,
    private val moneyOperRepo: MoneyOperRepo,
    private val moneyOperItemRepo: MoneyOperItemRepo,
    private val tagRepo: TagRepo,
    @Qualifier("conversionService") private val conversionService: ConversionService
) {
    @RequestMapping
    @Transactional(readOnly = true)
    fun getMoneyOpers(
            @RequestParam(required = false, defaultValue = "") search: String,
            @RequestParam(required = false, defaultValue = "10") limit: Int,
            @RequestParam(required = false, defaultValue = "0") offset: Int): HmResponse {
        return try {
            val opers = ArrayList<MoneyOperDto>()
            val balanceSheet = apiRequestContextHolder.getBalanceSheet()
            if (offset == 0) {
                val pendingOpers = getMoneyOpers(balanceSheet, MoneyOperStatus.pending, search, limit + 1, offset)
                        .map { conversionService.convert(it, MoneyOperDto::class.java)!! }
                opers.addAll(pendingOpers)
                val beforeDate = LocalDate.now().plusDays(30)
                val recurrenceOpers = moneyOperService.getNextRecurrenceOpers(balanceSheet, search, beforeDate)
                        .map { conversionService.convert(it, MoneyOperDto::class.java)!! }
                opers.addAll(recurrenceOpers)
                opers.sortWith(Comparator.comparing(MoneyOperDto::operDate).reversed())
            }
            val doneOpers = getMoneyOpers(balanceSheet, MoneyOperStatus.done, search, limit + 1, offset)
                    .map { conversionService.convert(it, MoneyOperDto::class.java)!! }
            val hasNext = doneOpers.size > limit
            opers.addAll(if (hasNext) doneOpers.subList(0, limit) else doneOpers)
            val pagedList = PagedList(opers, limit, offset, hasNext)
            HmResponse.getOk(pagedList)
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    private fun getMoneyOpers(balanceSheet: BalanceSheet, status: MoneyOperStatus, search: String,
                              limit: Int, offset: Int): List<MoneyOper> {
        val sort = Sort.by(Sort.Direction.DESC, "performed")
                .and(Sort.by("dateNum"))
                .and(Sort.by(Sort.Direction.DESC, "created"))
        return if (search.isBlank())
            getMoneyOpers(balanceSheet, status, sort, limit, offset)
        else
            getMoneyOpersBySearch(balanceSheet, status, search.lowercase(Locale.getDefault()), sort, limit, offset)
    }

    private fun getMoneyOpers(balanceSheet: BalanceSheet, status: MoneyOperStatus, sort: Sort, limit: Int,
                              offset: Int): List<MoneyOper> {
        val pageRequest: Pageable = PageRequest.of(offset / (limit - 1), limit - 1, sort)
        return moneyOperRepo.findByBalanceSheetAndStatus(balanceSheet, status, pageRequest).content.plus(
                moneyOperRepo.findByBalanceSheetAndStatus(balanceSheet, status, pageRequest.next()).content.take(1)
        )
    }

    private fun getMoneyOpersBySearch(balanceSheet: BalanceSheet, status: MoneyOperStatus, search: String,
                                      sort: Sort, limit: Int, offset: Int): List<MoneyOper> {
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
                    moneyOperRepo.findByBalanceSheetAndStatusAndPerformed(balanceSheet, status, LocalDate.parse(search), pageable)
                }
                adder = Consumer { p: Page<*> -> opers.addAll((p as Page<MoneyOper>).content) }
            }
            search.matches(SearchUuidRegex) -> {
                // по идентификатору операции
                pager = Function { pageable: Pageable ->
                    moneyOperRepo.findByBalanceSheetAndStatusAndId(balanceSheet, status, UUID.fromString(search), pageable)
                }
                adder = Consumer { p: Page<*> -> opers.addAll((p as Page<MoneyOper>).content) }
            }
            search.matches(SearchMoneyRegex) -> {
                // по сумме операции
                pageRequest = PageRequest.of(pageRequest.pageNumber, pageRequest.pageSize)
                pager = Function { pageable: Pageable ->
                    val value = BigDecimal(search).abs()
                    moneyOperItemRepo.findByBalanceSheetAndValueOrderByPerformedDesc(balanceSheet, value, pageable)
                }
                adder = Consumer { p: Page<*> ->
                    (p as Page<MoneyOperItem>).content
                            .map { it.moneyOper }
                            .filter { it.status !== status }
                            .distinct()
                            .forEach { e: MoneyOper -> opers.add(e) }
                }
            }
            else -> {
                pager = Function { pageable: Pageable? -> moneyOperRepo.findByBalanceSheetAndStatus(balanceSheet, status, pageable!!) }
                adder = Consumer { p: Page<*> ->
                    val opersChunk = (p as Page<MoneyOper>).content
                            .filter { oper: MoneyOper ->
                                (oper.items.any { item: MoneyOperItem -> item.balance.name.lowercase(Locale.getDefault())
                                    .contains(search) } // по комментарию
                                        || oper.comment?.lowercase(Locale.getDefault())?.contains(search) ?: false // по меткам
                                        || oper.tags.any { tagContains(it, search) })
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

    fun tagContains(tag: Tag, search: String): Boolean {
        return tag.name.lowercase(Locale.getDefault()).contains(search)
                || tag.isCategory!! && tag.rootId != null && tagContains(tagRepo.findByIdOrNull(tag.rootId)!!, search)
    }

    @RequestMapping("/item")
    @Transactional(readOnly = true)
    fun getMoneyOpers(@RequestParam id: UUID): HmResponse {
        return try {
            val oper = moneyOperRepo.findByIdOrNull(id)!!
            moneyOperService.checkMoneyOperBelongsBalanceSheet(oper, apiRequestContextHolder.getBsId())
            HmResponse.getOk(conversionService.convert(oper, MoneyOperDto::class.java)!!)
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    @RequestMapping("/create")
    fun createMoneyOper(@RequestBody moneyOperDto: MoneyOperDto): HmResponse {
        val moneyOperDtoList = createMoneyOperInternal(moneyOperDto)
                .map { conversionService.convert(it, MoneyOperDto::class.java) }
        return HmResponse.getOk(moneyOperDtoList)
    }

    @RequestMapping("/update")
    fun updateMoneyOper(@RequestBody moneyOperDto: MoneyOperDto): HmResponse {
        return try {
            val balanceSheet = apiRequestContextHolder.getBalanceSheet()
            val origOper = moneyOperRepo.findByIdOrNull(moneyOperDto.id)
            if (origOper == null) {
                createMoneyOperInternal(moneyOperDto)
                return HmResponse.getOk()
            }
            moneyOperService.checkMoneyOperBelongsBalanceSheet(origOper, balanceSheet.id)
            val oper = moneyOperService.moneyOperDtoToMoneyOper(balanceSheet, moneyOperDto)
            val essentialEquals = origOper.essentialEquals(oper)
            val origPrevStatus = origOper.status
            if (!essentialEquals && origOper.status == MoneyOperStatus.done) {
                origOper.cancel()
            }
            moneyOperService.updateMoneyOper(origOper, oper)
            if (!essentialEquals && origPrevStatus == MoneyOperStatus.done || origOper.status == MoneyOperStatus.pending
                    && moneyOperDto.status == MoneyOperStatus.done) {
                origOper.complete()
            }
            moneyOperRepo.save(origOper)
            HmResponse.getOk()
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    @RequestMapping("/delete")
    fun deleteMoneyOper(@RequestBody moneyOperDto: MoneyOperDto): HmResponse {
        return try {
            val oper = moneyOperRepo.findByIdOrNull(moneyOperDto.id)!!
            moneyOperService.checkMoneyOperBelongsBalanceSheet(oper, apiRequestContextHolder.getBsId())
            oper.cancel()
            moneyOperRepo.save(oper)
            HmResponse.getOk()
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    @RequestMapping("/skip")
    @Throws(SQLException::class)
    fun skipMoneyOper(@RequestBody moneyOperDto: MoneyOperDto): HmResponse {
        return try {
            val status = moneyOperDto.status
            if (status == MoneyOperStatus.pending) {
                skipPendingMoneyOper(apiRequestContextHolder.getBsId(), moneyOperDto)
            } else if (status == MoneyOperStatus.recurrence) {
                skipRecurrenceMoneyOper(moneyOperDto)
            }
            HmResponse.getOk()
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    private fun skipPendingMoneyOper(bsId: UUID, moneyOperDto: MoneyOperDto) {
        val oper = moneyOperRepo.findByIdOrNull(moneyOperDto.id)!!
        moneyOperService.checkMoneyOperBelongsBalanceSheet(oper, bsId)
        oper.cancel()
        oper.recurrenceId = null
        moneyOperRepo.save(oper)
    }

    private fun skipRecurrenceMoneyOper(moneyOperDto: MoneyOperDto) {
        moneyOperService.findRecurrenceOper(moneyOperDto.recurrenceId!!)?.let {
            it.skipNextDate()
            moneyOperService.save(it)
        }
    }

    @RequestMapping("/up")
    fun upMoneyOper(@RequestBody moneyOperDto: MoneyOperDto): HmResponse {
        return try {
            val oper = moneyOperRepo.findByIdOrNull(moneyOperDto.id)!!
            val opers = moneyOperRepo.findByBalanceSheetAndStatusAndPerformed(oper.balanceSheet, MoneyOperStatus.done, oper.performed)
                    .sortedBy { it.dateNum }
                    .toMutableList()
            val index = opers.indexOf(oper)
            if (index > 0) {
                val prevOper = opers[index - 1]
                opers[index - 1] = oper
                opers[index] = prevOper
                IntStream.range(0, opers.size).forEach { i: Int ->
                    val o = opers[i]
                    o.dateNum = i
                    moneyOperRepo.save(o)
                }
            }
            HmResponse.getOk()
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    private fun createMoneyOperInternal(moneyOperDto: MoneyOperDto): List<MoneyOper> {
        val balanceSheet = apiRequestContextHolder.getBalanceSheet()
        val moneyOpers: MutableList<MoneyOper> = mutableListOf()
        val mainOper = newMainMoneyOper(balanceSheet, moneyOperDto)
        moneyOperRepo.save(mainOper)
        moneyOpers.add(mainOper)
        newReserveMoneyOper(balanceSheet, mainOper)?.let { moneyOpers.add(it) }
        mainOper.recurrenceId?.also { moneyOperService.skipRecurrenceOper(balanceSheet, it) }
        if ((moneyOperDto.status == MoneyOperStatus.done || moneyOperDto.status == MoneyOperStatus.doneNew)
                && !mainOper.performed.isAfter(LocalDate.now())) {
            moneyOpers.forEach { it.complete() }
        }
        moneyOpers.forEach { moneyOperService.save(it) }
        return moneyOpers
    }

    private fun newMainMoneyOper(balanceSheet: BalanceSheet, moneyOperDto: MoneyOperDto): MoneyOper =
            moneyOperService.moneyOperDtoToMoneyOper(balanceSheet, moneyOperDto)

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

    @RequestMapping(value = ["/suggest-tags"], method = [RequestMethod.GET])
    @Transactional(readOnly = true)
    fun suggestTags(
        @RequestParam operType: String,
        @RequestParam search: String?,
        @RequestParam tags: Array<String>?
    ): HmResponse {
        val bsId = apiRequestContextHolder.getBsId()
        val suggestTags = moneyOperService.getSuggestTags(bsId, operType, search, tags?.toList() ?: emptyList())
                .map(Tag::name)
        return HmResponse.getOk(suggestTags)
    }

    @RequestMapping(value = ["/tags"])
    @Transactional(readOnly = true)
    fun tags(): HmResponse {
        val tags = moneyOperService.getTags(apiRequestContextHolder.getBsId()).map(Tag::name)
        return HmResponse.getOk(tags)
    }

    companion object {
        private val SearchDateRegex = "\\p{Digit}{4}-\\p{Digit}{2}-\\p{Digit}{2}".toRegex()
        private val SearchUuidRegex = "\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}".toRegex()
        private val SearchMoneyRegex = "\\p{Digit}+\\.*\\p{Digit}*".toRegex()
    }

}