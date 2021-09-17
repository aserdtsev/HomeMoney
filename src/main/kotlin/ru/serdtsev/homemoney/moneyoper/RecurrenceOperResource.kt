package ru.serdtsev.homemoney.moneyoper

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.account.model.Reserve
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.common.HmException
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.moneyoper.model.*
import java.util.stream.Collectors

@RestController
@RequestMapping("/api/recurrence-opers")
@Transactional
class RecurrenceOperResource constructor(
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
                    .map { recurrenceOper: RecurrenceOper -> recurrenceOperToDto(recurrenceOper) }
                    .collect(Collectors.toList())
            HmResponse.getOk(list)
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    private fun recurrenceOperToDto(recurrenceOper: RecurrenceOper): RecurrenceOperDto {
        val oper = recurrenceOper.template
        val type = if (oper.type == MoneyOperType.transfer && oper.items.any { it.balance is Reserve }) {
            val operItem = oper.items.first { it.balance is Reserve }
            if (operItem.value.signum() > 0) MoneyOperType.income.name else MoneyOperType.expense.name
        } else
            oper.type.name
        val dto = RecurrenceOperDto(recurrenceOper.id, oper.id, oper.id,
                recurrenceOper.nextDate, oper.period!!, oper.comment, getStringsByTags(oper.tags), type)
        val items = oper.items
                .map { conversionService.convert(it, MoneyOperItemDto::class.java)!! }
                .sortedBy { it.value.multiply(it.sgn.toBigDecimal()) }
        dto.items = items
        return dto
    }

    private fun getStringsByTags(tags: Collection<Tag>): List<String> = tags.map(Tag::name)

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