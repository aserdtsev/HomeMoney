package ru.serdtsev.homemoney.port.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.domain.repository.RecurrenceOperRepository
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import ru.serdtsev.homemoney.port.dto.moneyoper.RecurrenceOperDto
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

internal class RecurrenceOperControllerTest : SpringBootBaseTest() {
    private val now = LocalDate.now()
    private val cashBalance = Balance(UUID.randomUUID(), AccountType.debit, "Наличные", now,
        false, balanceSheet.currencyCode, BigDecimal("100.00"))
    private val cardBalance = Balance(UUID.randomUUID(), AccountType.debit, "Карта", now,
        false, balanceSheet.currencyCode, BigDecimal("0.00"))
    private val foodstuffsTag = Tag("Продукты")
    private val fitnessTag = Tag("Фитнес")
    private val salaryTag = Tag("Зарплата")

    @Autowired
    @Qualifier("conversionService")
    private lateinit var conversionService: ConversionService

    @Autowired
    private lateinit var recurrenceOperRepository: RecurrenceOperRepository;

    @Autowired
    private lateinit var recurrenceOperController: RecurrenceOperController

    @BeforeEach
    fun setUp() {
        ApiRequestContextHolder.bsId = balanceSheet.id
        ApiRequestContextHolder.requestId = "REQUEST_ID"

        domainEventPublisher.publish(cashBalance)
        domainEventPublisher.publish(cardBalance)
        domainEventPublisher.publish(foodstuffsTag)
        domainEventPublisher.publish(salaryTag)
    }

    @Test
    fun updateRecurrenceOper() {
        val sample = MoneyOper(MoneyOperStatus.done, LocalDate.now().minusMonths(1),
            period = Period.month, tags = mutableListOf(foodstuffsTag), comment = "comment").apply {
            addItem(cashBalance, BigDecimal("-1.00"))
            domainEventPublisher.publish(this)
        }
        val recurrenceOper = RecurrenceOper.of(sample)
        DomainEventPublisher.instance.publish(recurrenceOper)

        val recurrenceOperDto = requireNotNull(
            conversionService.convert(recurrenceOper, RecurrenceOperDto::class.java))
        recurrenceOperDto.let {
            it.nextDate = it.nextDate.plusDays(1)
            it.period= Period.quarter
            it.items.first().let { item ->
                item.balanceId = cardBalance.id
                item.value = BigDecimal("-2.00")
            }
            it.comment = "new comment"
            it.tags = listOf(fitnessTag.name)
        }

        recurrenceOperController.updateRecurrenceOper(recurrenceOperDto)

        val actual = recurrenceOperRepository.findById(recurrenceOper.id)
        val expected = requireNotNull(conversionService.convert(recurrenceOperDto, RecurrenceOper::class.java))

        assertThat(actual)
            .usingRecursiveComparison()
            .isEqualTo(expected)
    }
}