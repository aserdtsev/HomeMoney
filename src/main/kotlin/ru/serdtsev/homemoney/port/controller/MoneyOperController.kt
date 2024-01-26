package ru.serdtsev.homemoney.port.controller

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.data.domain.*
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import ru.serdtsev.homemoney.domain.model.moneyoper.Tag
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.domain.usecase.moneyoper.*
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import ru.serdtsev.homemoney.infra.exception.HmException
import ru.serdtsev.homemoney.port.common.HmResponse
import ru.serdtsev.homemoney.port.common.PagedList
import ru.serdtsev.homemoney.port.dto.moneyoper.MoneyOperDto
import ru.serdtsev.homemoney.port.service.MoneyOperService
import ru.serdtsev.homemoney.port.service.TagService
import java.sql.SQLException
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/money-opers")
class MoneyOperController(
    private val createMoneyOperUseCase: CreateMoneyOperUseCase,
    private val updateMoneyOperUseCase: UpdateMoneyOperUseCase,
    private val deleteMoneyOperUseCase: DeleteMoneyOperUseCase,
    private val skipMoneyOperUseCase: SkipMoneyOperUseCase,
    private val upMoneyOperUseCase: UpMoneyOperUseCase,
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val moneyOperService: MoneyOperService,
    private val moneyOperRepository: MoneyOperRepository,
    private val tagService: TagService,
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
            if (offset == 0) {
                val beforeDate = LocalDate.now().plusDays(30)
                val upcomingOpers = moneyOperService.getMoneyOpers(MoneyOperStatus.Pending, search, Int.MAX_VALUE)
                    .plus(moneyOperService.getNextRecurrenceOpers(search, beforeDate))
                    .plus(moneyOperService.getUpcomingMoneyOpers(search)
                        .filter { it.performed < beforeDate })
                    .map { conversionService.convert(it, MoneyOperDto::class.java)!! }
                opers.addAll(upcomingOpers)
                opers.sortWith(Comparator.comparing(MoneyOperDto::operDate).reversed())
            }
            val doneOpers = moneyOperService.getMoneyOpers(MoneyOperStatus.Done, search, limit + 1, offset)
                    .map { conversionService.convert(it, MoneyOperDto::class.java)!! }
            val hasNext = doneOpers.size > limit
            opers.addAll(if (hasNext) doneOpers.subList(0, limit) else doneOpers)
            val pagedList = PagedList(opers, limit, offset, hasNext)
            HmResponse.getOk(pagedList)
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
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
        val suggestTags = tagService.getSuggestTags(operType, search, tags?.toList() ?: emptyList())
                .map(Tag::name)
        return HmResponse.getOk(suggestTags)
    }

    @RequestMapping(value = ["/tags"])
    @Transactional(readOnly = true)
    fun tags(): HmResponse {
        val tags = tagService.getTags(apiRequestContextHolder.getBsId()).map(Tag::name)
        return HmResponse.getOk(tags)
    }
}