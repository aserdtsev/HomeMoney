package ru.serdtsev.homemoney.moneyoper

import org.apache.commons.lang3.SerializationUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

internal class MoneyOperItemTest {
    private lateinit var cash: Balance
    private lateinit var checkingAccount: Balance
    private lateinit var oper: MoneyOper

    @BeforeEach
    fun setUp() {
        val balanceSheet = BalanceSheet()
        cash = Balance(UUID.randomUUID(), balanceSheet, AccountType.debit, "Cash", LocalDate.now(), false,
            "RUB", BigDecimal.TEN)
        checkingAccount = Balance(UUID.randomUUID(), balanceSheet, AccountType.debit, "Checking account",
                LocalDate.now(), false, "RUB", BigDecimal.valueOf(1000L))
        oper = MoneyOper(UUID.randomUUID(), balanceSheet, mutableListOf(), MoneyOperStatus.pending, LocalDate.now(), 0,
               listOf(), "", null)
    }

    @Test
    @Disabled
    fun essentialEquals() {
        val origItem = oper.addItem(cash, BigDecimal.ONE.negate())
        var item = SerializationUtils.clone(origItem)
        Assertions.assertTrue(item.mostlyEquals(origItem))
        item.performed = LocalDate.now().minusDays(1L)
        item.index = item.index + 1
        Assertions.assertTrue(item.mostlyEquals(origItem))
        item = SerializationUtils.clone(origItem)
        item.balance = checkingAccount
        Assertions.assertFalse(item.mostlyEquals(origItem))
        item = SerializationUtils.clone(origItem)
        item.value = origItem.value.add(BigDecimal.ONE)
        Assertions.assertFalse(item.mostlyEquals(origItem))
    }
}