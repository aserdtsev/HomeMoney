package ru.serdtsev.homemoney.port.controller

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.data.domain.*
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
    @GetMapping("/done")
    fun getDoneMoneyOpers(
        @RequestParam(required = false, defaultValue = "") search: String,
        @RequestParam(required = false, defaultValue = "5") limit: Int,
        @RequestParam(required = false, defaultValue = "0") offset: Int): HmResponse {
        return try {
            val opers = ArrayList<MoneyOperDto>()
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

    @GetMapping("/upcoming")
    fun getUpcomingMoneyOpers(
        @RequestParam(required = false, defaultValue = "") search: String,
        @RequestParam(required = false, defaultValue = "5") limit: Int,
        @RequestParam(required = false, defaultValue = "0") offset: Int): HmResponse {
        return try {
            val beforeDate = LocalDate.now().plusMonths(2)
            var opers = moneyOperService.getMoneyOpers(MoneyOperStatus.Pending, search, Int.MAX_VALUE)
                .plus(moneyOperService.getNextRecurrenceOpers(search, beforeDate))
                .plus(moneyOperService.getUpcomingMoneyOpers(search))
                .map { conversionService.convert(it, MoneyOperDto::class.java)!! }
                .toMutableList()
            val hasNext = opers.size > limit
            opers.sortWith(Comparator.comparing(MoneyOperDto::operDate).reversed())
            val upTodayOpers = opers.filter { it.operDate <= LocalDate.now() }.toMutableList()
            opers = if (offset == 0 && upTodayOpers.size <= limit) upTodayOpers
            else with (opers.lastIndex) {
                if (this > -1) opers.subList(Math.max(this - limit - offset, 0), this - offset) else opers
            }
            val pagedList = PagedList(opers, limit, offset, hasNext)
            HmResponse.getOk(pagedList)
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    @RequestMapping("/item")
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
    fun tags(): HmResponse {
        val tags = tagService.getTags(apiRequestContextHolder.getBsId()).map(Tag::name)
        return HmResponse.getOk(tags)
    }
}