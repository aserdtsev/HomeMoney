package ru.serdtsev.homemoney.port.controller

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import ru.serdtsev.homemoney.infra.exception.HmException
import ru.serdtsev.homemoney.port.common.HmResponse
import ru.serdtsev.homemoney.port.dto.moneyoper.MoneyOperDto
import ru.serdtsev.homemoney.port.dto.moneyoper.RecurrenceOperDto
import ru.serdtsev.homemoney.domain.model.moneyoper.RecurrenceOper
import ru.serdtsev.homemoney.domain.usecase.moneyoper.CreateRecurrenceOperUseCase
import ru.serdtsev.homemoney.domain.usecase.moneyoper.DeleteRecurrenceOperUseCase
import ru.serdtsev.homemoney.domain.usecase.moneyoper.SkipRecurrenceOperUseCase
import ru.serdtsev.homemoney.domain.usecase.moneyoper.UpdateRecurrenceOperUseCase
import ru.serdtsev.homemoney.port.service.MoneyOperService
import java.util.stream.Collectors

@RestController
@RequestMapping("/api/recurrence-opers")
@Transactional
class RecurrenceOperController(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val createRecurrenceOperUseCase: CreateRecurrenceOperUseCase,
    private val deleteRecurrenceOperUseCase: DeleteRecurrenceOperUseCase,
    private val skipRecurrenceOperUseCase: SkipRecurrenceOperUseCase,
    private val updateRecurrenceOperUseCase: UpdateRecurrenceOperUseCase,
    private val moneyOperService: MoneyOperService,
    @Qualifier("conversionService") private val conversionService: ConversionService
) {
    @RequestMapping
    @Transactional(readOnly = true)
    fun getList(@RequestParam(required = false, defaultValue = "") search: String): HmResponse {
        return try {
            val list = moneyOperService.getRecurrenceOpers(search)
                    .stream()
                    .sorted(Comparator.comparing(RecurrenceOper::nextDate))
                    .map { conversionService.convert(it, RecurrenceOperDto::class.java) }
                    .collect(Collectors.toList())
            HmResponse.getOk(list)
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    @RequestMapping("/create")
    fun create(@RequestBody moneyOperDto: MoneyOperDto): HmResponse {
        createRecurrenceOperUseCase.run(moneyOperDto.id)
        return HmResponse.getOk()
    }

    @RequestMapping("/skip")
    fun skip(@RequestBody oper: RecurrenceOperDto): HmResponse {
        skipRecurrenceOperUseCase.run(oper.id)
        return HmResponse.getOk()
    }

    @RequestMapping("/delete")
    fun delete(@RequestBody oper: RecurrenceOperDto): HmResponse {
        return try {
            deleteRecurrenceOperUseCase.run(oper.id)
            HmResponse.getOk()
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    @RequestMapping("/update")
    fun updateRecurrenceOper(@RequestBody recurrenceOperDto: RecurrenceOperDto): HmResponse {
        return try {
            val recurrenceOper = requireNotNull(
                conversionService.convert(recurrenceOperDto, RecurrenceOper::class.java))
            updateRecurrenceOperUseCase.run(recurrenceOper)
            HmResponse.getOk()
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }
}