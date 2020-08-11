package ru.serdtsev.homemoney.moneyoper

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import ru.serdtsev.homemoney.account.AccountRepository
import ru.serdtsev.homemoney.account.BalanceRepository
import ru.serdtsev.homemoney.account.CategoryRepository
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository
import ru.serdtsev.homemoney.moneyoper.model.Label
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperItem
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import ru.serdtsev.homemoney.moneyoper.model.Period
import java.math.BigDecimal
import java.sql.Date
import java.time.LocalDate
import java.util.*

internal class MoneyOperControllerTest {
    private val now = Date.valueOf(LocalDate.now())
    private val balanceSheet = BalanceSheet.newInstance()
    private val balance = Balance(UUID.randomUUID(), balanceSheet, AccountType.debit, "Cash", now, false,
            "RUB", BigDecimal.valueOf(10000L, 2))
    private val account = Balance(UUID.randomUUID(), balanceSheet, AccountType.credit, "Current account",
        now, false, "RUB", BigDecimal.valueOf(10000L, 2))
    private val accountRepo: AccountRepository = mock {
        whenever(it.findById(balanceSheet.uncatCosts!!.id)).thenReturn(Optional.of(balanceSheet.uncatCosts!!))
        whenever(it.findById(balanceSheet.uncatIncome!!.id)).thenReturn(Optional.of(balanceSheet.uncatIncome!!))
        whenever(it.findById(balance.id)).thenReturn(Optional.of(balance))
        whenever(it.findById(account.id)).thenReturn(Optional.of(account))
    }
    private val moneyOperService: MoneyOperService = mock {  }
    private val balanceSheetRepo: BalanceSheetRepository = mock {  }
    private val balanceRepo: BalanceRepository = mock {  }
    private val moneyOperRepo: MoneyOperRepo = mock {  }
    private val labelRepo: LabelRepository = mock {  }
    private val moneyOperItemRepo: MoneyOperItemRepo = mock {  }
    private val categoryRepo: CategoryRepository = mock {  }
    private val moneyOperController = MoneyOperController(moneyOperService, balanceSheetRepo, accountRepo, balanceRepo,
            moneyOperRepo, labelRepo, moneyOperItemRepo, categoryRepo)

    @BeforeEach
    fun setUp() {
    }

    @Test
    @Disabled
    fun newMoneyOper_simpleExpense() {
        val labels: MutableList<Label> = ArrayList()
        labels.add(Label(UUID.randomUUID(), balanceSheet, "label"))
        val performed = LocalDate.now()
        val comment = "my comment"
        val oper = moneyOperService.newMoneyOper(balanceSheet, UUID.randomUUID(), MoneyOperStatus.done,
                performed, 0, labels, comment, Period.month, balance.id, balanceSheet.uncatCosts!!.id,
                BigDecimal.ONE, BigDecimal.ONE)
        assertEquals(balanceSheet, oper.balanceSheet)
        val items = oper.items
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals(oper, item.moneyOper)
        assertEquals(balance, item.balance)
        assertEquals(BigDecimal.ONE.negate(), item.value)
        assertEquals(0, item.index)
        assertEquals(performed, item.performed)
        assertEquals(balance.id, oper.fromAccId)
        assertEquals(balanceSheet.uncatCosts!!.id, oper.toAccId)
        assertEquals(Period.month, oper.period)
        assertEquals(MoneyOperStatus.done, oper.status)
        assertEquals(comment, oper.comment)
        assertEquals(labels, oper.labels)
        assertEquals(0, oper.dateNum)
    }

    @Test
    @Disabled
    fun newMoneyOper_simpleIncome() {
        val performed = LocalDate.now()
        val oper = moneyOperService.newMoneyOper(balanceSheet, UUID.randomUUID(), MoneyOperStatus.done,
                performed, 0, emptyList(),"", Period.month, balanceSheet.uncatIncome!!.id,
                account.id, BigDecimal.ONE, BigDecimal.ONE)
        val items = oper.items
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals(account, item.balance)
        assertEquals(BigDecimal.ONE, item.value)
    }

    @Test
    @Disabled
    fun newMoneyOper_simpleTransfer() {
        val performed = LocalDate.now()
        val oper = moneyOperService.newMoneyOper(balanceSheet, UUID.randomUUID(), MoneyOperStatus.done,
                performed, 0, emptyList(),"", Period.month, account.id, balance.id, BigDecimal.ONE,
                BigDecimal.ONE)
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