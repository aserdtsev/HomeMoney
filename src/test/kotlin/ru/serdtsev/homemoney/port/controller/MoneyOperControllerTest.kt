package ru.serdtsev.homemoney.port.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.domain.repository.BalanceRepository
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import ru.serdtsev.homemoney.infra.dao.RecurrenceOperDao
import ru.serdtsev.homemoney.port.dto.moneyoper.MoneyOperDto
import ru.serdtsev.homemoney.port.dto.moneyoper.MoneyOperItemDto
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

internal class MoneyOperControllerTest: SpringBootBaseTest() {
    private val now = LocalDate.now()
    private val cashBalance = Balance(UUID.randomUUID(), AccountType.debit, "Наличные", now,
        false, balanceSheet.currencyCode, BigDecimal("100.00"))
    private val cardBalance = Balance(UUID.randomUUID(), AccountType.debit, "Карта", now,
        false, balanceSheet.currencyCode, BigDecimal("0.00"))
    private val foodstuffsTag = Tag(balanceSheet, "Продукты")
    private val salaryTag = Tag(balanceSheet, "Зарплата")
    @Autowired
    private lateinit var balanceRepository: BalanceRepository
    @Autowired
    private lateinit var moneyOperRepository: MoneyOperRepository
    @Autowired
    private lateinit var recurrenceOperDao: RecurrenceOperDao
    @Autowired
    @Qualifier("conversionService")
    private lateinit var conversionService: ConversionService
    @Autowired
    private lateinit var moneyOperController: MoneyOperController

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
    fun createMoneyOper_simpleExpense() {
        val template = MoneyOper(MoneyOperStatus.done, LocalDate.now().minusMonths(1), period = Period.month)
        domainEventPublisher.publish(template)
        val recurrenceOper = RecurrenceOper(template.id, LocalDate.now())
        domainEventPublisher.publish(recurrenceOper)

        val moneyOper = MoneyOper(MoneyOperStatus.doneNew,
            tags = mutableListOf(foodstuffsTag),
            comment = "comment",
            period = Period.single).apply { this.recurrenceId = recurrenceOper.id }
        moneyOper.addItem(cashBalance, BigDecimal("-1.00"))
        val moneyOperDto = conversionService.convert(moneyOper, MoneyOperDto::class.java)!!

        moneyOperController.createMoneyOper(moneyOperDto)

        val actualMoneyOper = moneyOperRepository.findById(moneyOperDto.id)
        val expectedItems =  conversionService.convert(moneyOperDto, MoneyOper::class.java)!!.items
        assertThat(actualMoneyOper)
            .isNotNull
            .extracting("items", "status", "performed", "comment", "period", "tags")
            .contains(expectedItems, MoneyOperStatus.done, now, moneyOperDto.comment, Period.single, mutableSetOf(foodstuffsTag))

        val actualBalance = balanceRepository.findById(cashBalance.id)
        assertEquals(BigDecimal("99.00"), actualBalance.value)

        val expectedRecurrenceOper = recurrenceOperDao.findById(actualMoneyOper.recurrenceId!!)
        assertEquals(LocalDate.now().plusMonths(1), expectedRecurrenceOper.nextDate)
    }

    @Test
    @Ignore
    fun createMoneyOper_simpleIncome() {
        val moneyOperDto = MoneyOperDto(UUID.randomUUID(), MoneyOperStatus.doneNew, LocalDate.now(), Period.single,
            "comment", listOf("Зарплата"), 0, null, null)
        val moneyOperItemDto = MoneyOperItemDto(UUID.randomUUID(), cashBalance.id, cashBalance.name, BigDecimal("1.00"),
            1, balanceSheet.currencyCode, now)
        moneyOperDto.items = mutableListOf(moneyOperItemDto)

        moneyOperController.createMoneyOper(moneyOperDto)

        val actualMoneyOper = moneyOperRepository.findById(moneyOperDto.id)
        val expectedItems = mutableListOf(
            MoneyOperItem(moneyOperItemDto.id, moneyOperDto.id, cashBalance, moneyOperItemDto.value,
                moneyOperItemDto.performedAt, 0)
        )
        assertThat(actualMoneyOper)
            .isNotNull
            .extracting("items", "status", "performed", "comment", "period", "tags")
            .contains(expectedItems, MoneyOperStatus.done, now, moneyOperDto.comment, Period.single,
                mutableSetOf(salaryTag))

        val actualBalance = balanceRepository.findById(cashBalance.id)
        assertEquals(BigDecimal("101.00"), actualBalance.value)
    }

    @Test
    @Ignore
    fun createMoneyOper_transfer() {
        val moneyOperDto = MoneyOperDto(UUID.randomUUID(), MoneyOperStatus.doneNew, LocalDate.now(), Period.single,
            "comment", listOf(), 0, null, null)
        val moneyOperItemDto1 = MoneyOperItemDto(UUID.randomUUID(), cashBalance.id, cashBalance.name, BigDecimal("1.00"),
            -1, balanceSheet.currencyCode, moneyOperDto.operDate)
        val moneyOperItemDto2 = MoneyOperItemDto(UUID.randomUUID(), cardBalance.id, cardBalance.name, BigDecimal("1.00"),
            1, balanceSheet.currencyCode, moneyOperDto.operDate)
        moneyOperDto.items = mutableListOf(moneyOperItemDto1, moneyOperItemDto2)

        moneyOperController.createMoneyOper(moneyOperDto)

        val actualMoneyOper = moneyOperRepository.findById(moneyOperDto.id)
        val expectedItems = mutableListOf(
            MoneyOperItem(moneyOperItemDto1.id, moneyOperDto.id, cashBalance, moneyOperItemDto1.value,
                moneyOperItemDto1.performedAt, 0),
            MoneyOperItem(moneyOperItemDto2.id, moneyOperDto.id, cashBalance, moneyOperItemDto2.value,
                moneyOperItemDto2.performedAt, 1)
        )
        assertThat(actualMoneyOper)
            .isNotNull
            .extracting("items", "status", "performed", "comment", "period", "tags")
            .contains(expectedItems, MoneyOperStatus.done, now, moneyOperDto.comment, Period.single, mutableSetOf<Tag>())

        assertEquals(BigDecimal("99.00"), balanceRepository.findById(cashBalance.id).value)
        assertEquals(BigDecimal("1.00"), balanceRepository.findById(cardBalance.id).value)
    }

    @Test
    internal fun updateMoneyOper() {
        val origMoneyOper = MoneyOper(MoneyOperStatus.done, now, 0, mutableSetOf(salaryTag),
            "Comment 1", Period.month)
        origMoneyOper.addItem(cashBalance, BigDecimal("1.00"), now, 0)
        domainEventPublisher.publish(origMoneyOper)

        val moneyOperDto = conversionService.convert(origMoneyOper, MoneyOperDto::class.java)!!.apply {
            this.period = Period.single
            this.comment = "Comment 2"
            this.tags = listOf("Зарплата")
            this.items[0].apply {
                this.balanceId = cardBalance.id
                this.value = BigDecimal("2.00")
            }
        }

        moneyOperController.updateMoneyOper(moneyOperDto)

        val actualMoneyOper = moneyOperRepository.findById(moneyOperDto.id)
        val expectedItems = conversionService.convert(moneyOperDto, MoneyOper::class.java)!!.items
        assertThat(actualMoneyOper)
            .isNotNull
            .extracting("items", "status", "performed", "comment", "period", "tags")
            .contains(expectedItems, MoneyOperStatus.done, now, moneyOperDto.comment, Period.single, mutableSetOf(salaryTag))

        val actualCashBalance = balanceRepository.findById(cashBalance.id)
        assertEquals(BigDecimal("99.00"), actualCashBalance.value)

        val actualCardBalance = balanceRepository.findById(cardBalance.id)
        assertEquals(BigDecimal("2.00"), actualCardBalance.value)
    }

    @Test
    internal fun deleteMoneyOper() {
        val moneyOper = MoneyOper(MoneyOperStatus.done).apply {
            this.addItem(cashBalance, BigDecimal("1.00"))
        }
        domainEventPublisher.publish(moneyOper)

        val moneyOperDto = conversionService.convert(moneyOper, MoneyOperDto::class.java)!!

        moneyOperController.deleteMoneyOper(moneyOperDto)

        val actualMoneyOper = moneyOperRepository.findById(moneyOperDto.id)
        assertEquals(MoneyOperStatus.cancelled, actualMoneyOper.status)

        val actualBalance = balanceRepository.findById(cashBalance.id)
        assertEquals(BigDecimal("99.00"), actualBalance.value)
    }

    @Test
    internal fun skipMoneyOper() {
        val moneyOper = MoneyOper(MoneyOperStatus.pending)
        moneyOper.addItem(cashBalance, BigDecimal("1.00"))
        domainEventPublisher.publish(moneyOper)

        val moneyOperDto = conversionService.convert(moneyOper, MoneyOperDto::class.java)!!

        moneyOperController.skipMoneyOper(moneyOperDto)

        val actualMoneyOper = moneyOperRepository.findById(moneyOperDto.id)
        assertEquals(MoneyOperStatus.cancelled, actualMoneyOper.status)
    }

    @Test
    internal fun upMoneyOper() {
        val moneyOpers = IntRange(0, 2)
            .map { i ->
                MoneyOper(MoneyOperStatus.done, dateNum = i).apply {
                    domainEventPublisher.publish(this)
                }
            }
            .toList()
        val moneyOper = moneyOpers[0]
        val nextMoneyOper = moneyOpers[1]
        val moneyOperDto = conversionService.convert(moneyOper, MoneyOperDto::class.java)!!

        moneyOperController.upMoneyOper(moneyOperDto)

        assertEquals(1, moneyOperRepository.findById(moneyOper.id).dateNum)
        assertEquals(0, moneyOperRepository.findById(nextMoneyOper.id).dateNum)
    }
}