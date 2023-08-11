package ru.serdtsev.homemoney.port.controller

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.data.domain.*
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperItem
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import ru.serdtsev.homemoney.domain.model.moneyoper.Tag
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.domain.repository.TagRepository
import ru.serdtsev.homemoney.domain.usecase.moneyoper.*
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import ru.serdtsev.homemoney.infra.exception.HmException
import ru.serdtsev.homemoney.port.common.HmResponse
import ru.serdtsev.homemoney.port.common.PagedList
import ru.serdtsev.homemoney.port.dto.moneyoper.MoneyOperDto
import ru.serdtsev.homemoney.port.service.MoneyOperService
import java.math.BigDecimal
import java.sql.SQLException
import java.time.LocalDate
import java.util.*
import java.util.function.Consumer
import java.util.function.Function

@RestController
@RequestMapping("/api/money-opers")
@Transactional
class MoneyOperController(
    private val createMoneyOperUseCase: CreateMoneyOperUseCase,
    private val updateMoneyOperUseCase: UpdateMoneyOperUseCase,
    private val deleteMoneyOperUseCase: DeleteMoneyOperUseCase,
    private val skipMoneyOperUseCase: SkipMoneyOperUseCase,
    private val upMoneyOperUseCase: UpMoneyOperUseCase,
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val moneyOperService: MoneyOperService,
    private val moneyOperRepository: MoneyOperRepository,
    private val tagRepository: TagRepository,
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
        val sort = Sort.by(Sort.Direction.DESC, "trn_date")
                .and(Sort.by(Sort.Direction.DESC,"date_num"))
                .and(Sort.by(Sort.Direction.DESC, "created_ts"))
        return if (search.isBlank())
            getMoneyOpers(balanceSheet, status, sort, limit, offset)
        else
            getMoneyOpersBySearch(balanceSheet, status, search.lowercase(Locale.getDefault()), sort, limit, offset)
    }

    private fun getMoneyOpers(balanceSheet: BalanceSheet, status: MoneyOperStatus, sort: Sort, limit: Int,
                              offset: Int): List<MoneyOper> {
        val pageRequest: Pageable = PageRequest.of(offset / (limit - 1), limit - 1, sort)
        return moneyOperRepository.findByBalanceSheetAndStatus(balanceSheet, status, pageRequest).content.plus(
                moneyOperRepository.findByBalanceSheetAndStatus(balanceSheet, status, pageRequest.next()).content.take(1)
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
                    moneyOperRepository.findByBalanceSheetAndStatusAndPerformed(balanceSheet, status, LocalDate.parse(search), pageable)
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
                    moneyOperRepository.findByBalanceSheetAndValueOrderByPerformedDesc(balanceSheet, value, pageable)
                }
                adder = Consumer { p: Page<*> ->
                    (p as Page<MoneyOper>).content
                            .filter { it.status !== status }
                            .forEach { e: MoneyOper -> opers.add(e) }
                }
            }
            else -> {
                pager = Function { pageable: Pageable? -> moneyOperRepository.findByBalanceSheetAndStatus(balanceSheet, status, pageable!!) }
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
        return tag.name.lowercase().contains(search) ||
                tag.isCategory && tag.rootId?.let { tagRepository.findByIdOrNull(it) }
            ?.let { tagContains(it, search) } ?: false
    }

    @RequestMapping("/item")
    @Transactional(readOnly = true)
    fun getMoneyOpers(@RequestParam id: UUID): HmResponse {
        return try {
            val oper = moneyOperRepository.findById(id)
            HmResponse.getOk(conversionService.convert(oper, MoneyOperDto::class.java)!!)
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    @RequestMapping("/create")
    fun createMoneyOper(@RequestBody moneyOperDto: MoneyOperDto): HmResponse {
        val moneyOper = conversionService.convert(moneyOperDto, MoneyOper::class.java)!!
        val moneyOperDtoList = createMoneyOperUseCase.run(moneyOper)
                .map { conversionService.convert(it, MoneyOperDto::class.java) }
        return HmResponse.getOk(moneyOperDtoList)
    }

    @RequestMapping("/update")
    fun updateMoneyOper(@RequestBody moneyOperDto: MoneyOperDto): HmResponse {
        return try {
            if (moneyOperRepository.findByIdOrNull(moneyOperDto.id) == null) {
                throw IllegalStateException("MoneyOper ${moneyOperDto.id} not exists")
            }
            val moneyOper = conversionService.convert(moneyOperDto, MoneyOper::class.java)!!
            updateMoneyOperUseCase.run(moneyOper)

            HmResponse.getOk()
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    @RequestMapping("/delete")
    fun deleteMoneyOper(@RequestBody moneyOperDto: MoneyOperDto): HmResponse {
        return try {
            deleteMoneyOperUseCase.run(moneyOperDto.id)
            HmResponse.getOk()
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    @RequestMapping("/skip")
    @Throws(SQLException::class)
    fun skipMoneyOper(@RequestBody moneyOperDto: MoneyOperDto): HmResponse {
        return try {
            val moneyOper = conversionService.convert(moneyOperDto, MoneyOper::class.java)!!
            skipMoneyOperUseCase.run(moneyOper)
            HmResponse.getOk()
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    @RequestMapping("/up")
    fun upMoneyOper(@RequestBody moneyOperDto: MoneyOperDto): HmResponse {
        return try {
            upMoneyOperUseCase.run(moneyOperDto.id)
            HmResponse.getOk()
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    @RequestMapping(value = ["/suggest-tags"], method = [RequestMethod.GET])
    @Transactional(readOnly = true)
    fun suggestTags(
        @RequestParam operType: String,
        @RequestParam search: String?,
        @RequestParam tags: Array<String>?
    ): HmResponse {
        val suggestTags = moneyOperService.getSuggestTags(operType, search, tags?.toList() ?: emptyList())
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
        private val SearchDateRegex = "\\d{4}-\\d{2}-\\d{2}".toRegex()
        private val SearchUuidRegex = "\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}".toRegex()
        private val SearchMoneyRegex = "\\d+\\.*\\d*".toRegex()
    }

}