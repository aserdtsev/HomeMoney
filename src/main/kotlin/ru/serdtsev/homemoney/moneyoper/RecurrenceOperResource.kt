package ru.serdtsev.homemoney.moneyoper

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import ru.serdtsev.homemoney.account.AccountRepository
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository
import ru.serdtsev.homemoney.common.HmException
import ru.serdtsev.homemoney.common.HmResponse
import ru.serdtsev.homemoney.moneyoper.model.*
import java.util.*
import java.util.stream.Collectors

@RestController
@RequestMapping("/api/{bsId}/recurrence-opers")
@Transactional
class RecurrenceOperResource @Autowired constructor(
        private val moneyOperService: MoneyOperService,
        private val balanceSheetRepo: BalanceSheetRepository,
        private val accountRepo: AccountRepository,
        @Qualifier("conversionService") private val conversionService: ConversionService) {
    @RequestMapping
    @Transactional(readOnly = true)
    fun getList(
            @PathVariable bsId: UUID,
            @RequestParam(required = false, defaultValue = "") search: String
    ): HmResponse {
        return try {
            val balanceSheet = balanceSheetRepo.findByIdOrNull(bsId)!!
            val list = moneyOperService.getRecurrenceOpers(balanceSheet, search)
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
        val dto = RecurrenceOperDto(recurrenceOper.id, oper.id, oper.id,
                recurrenceOper.nextDate, oper.period!!, oper.comment, getStringsByTags(oper.tags), oper.type.name)
        val items = oper.items
                .map { conversionService.convert(it, MoneyOperItemDto::class.java)!! }
                .sortedBy { it.value.multiply(it.sgn.toBigDecimal()) }
        dto.items = items
        return dto
    }

    private fun getStringsByTags(tags: Collection<Tag>): List<String> = tags.map(Tag::name)

    @RequestMapping("/create")
    fun create(
            @PathVariable bsId: UUID,
            @RequestBody moneyOperDto: MoneyOperDto
    ): HmResponse {
        val balanceSheet = balanceSheetRepo.findByIdOrNull(bsId)!!
        moneyOperService.createRecurrenceOper(balanceSheet, moneyOperDto.id)
        return HmResponse.getOk()
    }

    @RequestMapping("/skip")
    fun skip(
            @PathVariable bsId: UUID,
            @RequestBody oper: RecurrenceOperDto
    ): HmResponse {
        val balanceSheet = balanceSheetRepo.findByIdOrNull(bsId)!!
        moneyOperService.skipRecurrenceOper(balanceSheet, oper.id)
        return HmResponse.getOk()
    }

    @RequestMapping("/delete")
    fun delete(
            @PathVariable bsId: UUID,
            @RequestBody oper: RecurrenceOperDto
    ): HmResponse {
        return try {
            val balanceSheet = balanceSheetRepo.findByIdOrNull(bsId)!!
            moneyOperService.deleteRecurrenceOper(balanceSheet, oper.id)
            HmResponse.getOk()
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }

    @RequestMapping("/update")
    fun updateRecurrenceOper(
            @PathVariable bsId: UUID,
            @RequestBody oper: RecurrenceOperDto?
    ): HmResponse {
        return try {
            val balanceSheet = balanceSheetRepo.findByIdOrNull(bsId)!!
            moneyOperService.updateRecurrenceOper(balanceSheet, oper!!)
            HmResponse.getOk()
        } catch (e: HmException) {
            HmResponse.getFail(e.code.name)
        }
    }
}