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
import ru.serdtsev.homemoney.domain.model.account.Credit
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.domain.repository.BalanceRepository
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.infra.dao.RecurrenceOperDao
import ru.serdtsev.homemoney.port.dto.moneyoper.MoneyOperDto
import ru.serdtsev.homemoney.port.dto.moneyoper.MoneyOperItemDto
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

internal class MoneyOperControllerTest : SpringBootBaseTest() {
    private val now = LocalDate.now()
    private val cashBalance = Balance(UUID.randomUUID(), AccountType.debit, "Наличные", now,
        false, balanceSheet.currencyCode, BigDecimal("100.00"))
    private val cardBalance = run {
        val credit = Credit(BigDecimal("400000.00"), 12, 55)
        Balance(UUID.randomUUID(), AccountType.debit, "Карта", now,
            false, balanceSheet.currencyCode, BigDecimal("0.00"), credit = credit)
    }
    private lateinit var foodstuffsTag: Tag
    private lateinit var salaryTag: Tag

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
        foodstuffsTag = Tag.of("Продукты")
        salaryTag = Tag.of("Зарплата")
        domainEventPublisher.publish(cashBalance)
        domainEventPublisher.publish(cardBalance)
    }

    @Test
    fun createMoneyOper_simpleExpense() {
        val balance = cardBalance
        val sample = MoneyOper(MoneyOperStatus.done, LocalDate.now().minusMonths(1),
            tags = mutableListOf(foodstuffsTag), comment = "comment", period = Period.month).apply {
            addItem(cardBalance, BigDecimal("-1.00"))
            domainEventPublisher.publish(this)
        }
        val recurrenceOper = RecurrenceOper.of(sample)

        val moneyOper = recurrenceOper.createNextMoneyOper().apply {
            status = MoneyOperStatus.doneNew
        }
        val moneyOperDto = conversionService.convert(moneyOper, MoneyOperDto::class.java)!!

        moneyOperController.createMoneyOper(moneyOperDto)

        val actualMoneyOper = moneyOperRepository.findById(moneyOperDto.id)
        val expectedItems =  conversionService.convert(moneyOperDto, MoneyOper::class.java)!!.items
        assertThat(actualMoneyOper)
            .isNotNull
            .extracting("items", "status", "performed", "comment", "period", "tags")
            .contains(expectedItems, MoneyOperStatus.done, now, moneyOperDto.comment, Period.month, mutableSetOf(foodstuffsTag))

        val actualBalance = balanceRepository.findById(balance.id)
        assertEquals(BigDecimal("-1.00"), actualBalance.value)

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
            MoneyOperItem.of(moneyOperDto.id, cashBalance, moneyOperItemDto.value,
                moneyOperItemDto.performedAt, 0, id = moneyOperItemDto.id)
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
            MoneyOperItem.of(moneyOperDto.id, cashBalance, moneyOperItemDto1.value, moneyOperItemDto1.performedAt,
                0, id = moneyOperItemDto1.id),
            MoneyOperItem.of(moneyOperDto.id, cashBalance, moneyOperItemDto2.value, moneyOperItemDto2.performedAt,
                1, id = moneyOperItemDto2.id)
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
        val origMoneyOper = MoneyOper(MoneyOperStatus.done, now, mutableSetOf(salaryTag), "Comment 1",
            Period.month)
        origMoneyOper.addItem(cashBalance, BigDecimal("-1.00"), now, 0)
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

        assertEquals(BigDecimal("101.00"), balanceRepository.findById(cashBalance.id).value)
        assertEquals(BigDecimal("-2.00"), balanceRepository.findById(cardBalance.id).value)

        moneyOperDto.items[0].value = BigDecimal("3.00")
        moneyOperController.updateMoneyOper(moneyOperDto)
        assertEquals(BigDecimal("-3.00"), balanceRepository.findById(cardBalance.id).value)
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