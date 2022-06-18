package ru.serdtsev.homemoney.moneyoper

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.core.convert.ConversionService
import ru.serdtsev.homemoney.account.dao.AccountDao
import ru.serdtsev.homemoney.account.dao.BalanceDao
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.common.ApiRequestContextHolder
import ru.serdtsev.homemoney.moneyoper.dao.MoneyOperDao
import ru.serdtsev.homemoney.moneyoper.dao.MoneyOperItemDao
import ru.serdtsev.homemoney.moneyoper.dao.TagDao
import ru.serdtsev.homemoney.moneyoper.model.*
import ru.serdtsev.homemoney.moneyoper.service.MoneyOperService
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

internal class MoneyOperControllerTest {
    private val now = LocalDate.now()
    private val balanceSheet = BalanceSheet()
    private val balance = Balance(UUID.randomUUID(), balanceSheet, AccountType.debit, "Cash", now, false,
        "RUB", BigDecimal.valueOf(10000L, 2))
    private val account = Balance(UUID.randomUUID(), balanceSheet, AccountType.credit, "Current account",
        now, false, "RUB", BigDecimal.valueOf(10000L, 2))
    private val accountDao: AccountDao = mock {
        whenever(it.findNameById(balance.id)).thenReturn(balance.name)
        whenever(it.findNameById(account.id)).thenReturn(account.name) // todo Актуально?
    }
    private val moneyOperService: MoneyOperService = mock {  }
    private val moneyOperDao: MoneyOperDao = mock {  }
    private val tagDao: TagDao = mock {  }
    private val balanceDao: BalanceDao = mock {  }
    private val moneyOperItemDao: MoneyOperItemDao = mock {  }
    private val conversionService: ConversionService = mock {  }
    private val apiRequestContextHolder: ApiRequestContextHolder = mock {  }
    private val moneyOperController = MoneyOperController(apiRequestContextHolder, moneyOperService, moneyOperDao,
        moneyOperItemDao, tagDao, balanceDao, conversionService)

    @BeforeEach
    fun setUp() {
    }

    @Test
    @Disabled
    fun newMoneyOper_simpleExpense() {
        val tags: MutableList<Tag> = ArrayList()
        tags.add(Tag(UUID.randomUUID(), balanceSheet,"tag"))
        val performed = LocalDate.now()
        val comment = "my comment"
        val oper = MoneyOper(balanceSheet, MoneyOperStatus.done, period = Period.month)
        oper.addItem(balance, BigDecimal.ONE)
        assertEquals(balanceSheet, oper.balanceSheet)
        val items = oper.items
        assertEquals(1, items.size)
        val item = items[0]
//        assertEquals(oper, item.moneyOper)
        assertEquals(balance, item.balance)
        assertEquals(BigDecimal.ONE.negate(), item.value)
        assertEquals(0, item.index)
        assertEquals(performed, item.performed)
        assertEquals(balance, item.balance)
        assertEquals(Period.month, oper.period)
        assertEquals(MoneyOperStatus.done, oper.status)
        assertEquals(comment, oper.comment)
        assertEquals(tags, oper.tags)
        assertEquals(0, oper.dateNum)
    }

    @Test
    @Disabled
    fun newMoneyOper_simpleIncome() {
        val oper = MoneyOper(balanceSheet, MoneyOperStatus.done, period = Period.month)
        oper.addItem(account, BigDecimal.ONE)
        val items = oper.items
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals(account, item.balance)
        assertEquals(BigDecimal.ONE, item.value)
    }

    @Test
    @Disabled
    fun newMoneyOper_simpleTransfer() {
        val oper = MoneyOper(balanceSheet, MoneyOperStatus.done, period = Period.month)
        oper.addItem(account, BigDecimal.ONE.negate())
        oper.addItem(balance, BigDecimal.ONE)
        val items: List<MoneyOperItem> = oper.items
        assertEquals(2, items.size)
        val item0 = items[0]
        assertEquals(account, item0.balance)
        assertEquals(BigDecimal.ONE.negate(), item0.value)
        val item1 = items[1]
        assertEquals(balance, item1.balance)
        assertEquals(BigDecimal.ONE, item1.value)
    }
}