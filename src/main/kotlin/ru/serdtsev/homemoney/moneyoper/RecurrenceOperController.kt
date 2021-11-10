package ru.serdtsev.homemoney.moneyoper

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.common.HmException
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.moneyoper.dto.MoneyOperDto
import ru.serdtsev.homemoney.moneyoper.dto.RecurrenceOperDto
import ru.serdtsev.homemoney.moneyoper.model.RecurrenceOper
import ru.serdtsev.homemoney.moneyoper.service.MoneyOperService
import java.util.stream.Collectors

@RestController
@RequestMapping("/api/recurrence-opers")
@Transactional
class RecurrenceOperController constructor(
    private val apiRequestContextHolder: ApiRequestContextHolder,
    private val moneyOperService: MoneyOperService,
    @Qualifier("conversionService") private val conversionService: ConversionService
) {
    @RequestMapping
    @Transactional(readOnly = true)
    fun getList(@RequestParam(required = false, defaultValue = "") search: String): HmResponse {
        return try {
            val list = moneyOperService.getRecurrenceOpers(apiRequestContextHolder.getBalanceSheet(), search)
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
        moneyOperService.createRecurrenceOper(apiRequestContextHolder.getBalanceSheet(), moneyOperDto.id)
        return HmResponse.getOk()
    }

    @RequestMapping("/skip")
    fun skip(@RequestBody oper: RecurrenceOperDto): HmResponse {
        moneyOperService.skipRecurrenceOper(apiRequestContextHolder.getBalanceSheet(), oper.id)
        return HmResponse.getOk()
    }

    @RequestMapping("/delete")
    fun delete(@RequestBody oper: RecurrenceOperDto): HmResponse {
        return try {
            moneyOperService.deleteRecurrenceOper(apiRequestContextHolder.getBalanceSheet(), oper.id)
            HmResponse.getOk()
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    @RequestMapping("/update")
    fun updateRecurrenceOper(@RequestBody oper: RecurrenceOperDto?): HmResponse {
        return try {
            moneyOperService.updateRecurrenceOper(apiRequestContextHolder.getBalanceSheet(), oper!!)
            HmResponse.getOk()
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }
}