package ru.serdtsev.homemoney.domain.model.moneyoper

import org.apache.commons.lang3.SerializationUtils
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.domain.DomainBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

internal class MoneyOperItemTest : DomainBaseTest() {
    private lateinit var cash: Balance
    private lateinit var checkingAccount: Balance
    private lateinit var oper: MoneyOper

    @BeforeEach
    override fun setUp() {
        super.setUp()
        cash = Balance(UUID.randomUUID(), AccountType.debit, "Cash", LocalDate.now(), false,
            "RUB", BigDecimal.TEN)
        checkingAccount = Balance(UUID.randomUUID(), AccountType.debit, "Checking account",
                LocalDate.now(), false, "RUB", BigDecimal.valueOf(1000L))
        oper = MoneyOper(UUID.randomUUID(), mutableListOf(), MoneyOperStatus.Pending, LocalDate.now(), listOf(),
            "", null, dateNum = 0)
    }

    @Test
    fun balanceEquals() {
        val origItem = oper.addItem(cash, BigDecimal.ONE.negate())
        var item = SerializationUtils.clone(origItem)
        assertTrue(MoneyOperItem.balanceEquals(item, origItem))
        item.performed = LocalDate.now().minusDays(1L)
        item.index = item.index + 1
        assertTrue(MoneyOperItem.balanceEquals(item, origItem))
        item = SerializationUtils.clone(origItem)
        item.balanceId = checkingAccount.id
        assertFalse(MoneyOperItem.balanceEquals(item, origItem))
        item = SerializationUtils.clone(origItem)
        item.value = origItem.value.add(BigDecimal.ONE)
        assertFalse(MoneyOperItem.balanceEquals(item, origItem))
    }
}