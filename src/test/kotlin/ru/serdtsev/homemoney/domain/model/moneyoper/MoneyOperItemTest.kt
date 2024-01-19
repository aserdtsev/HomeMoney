package ru.serdtsev.homemoney.domain.model.moneyoper

import org.apache.commons.lang3.SerializationUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.domain.DomainBaseTest
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.account.Credit
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

    @Test
    internal fun earlyRepaymentDebt() {
        val credit = Credit(BigDecimal("700000.00"), 12, 6)
        val balance = Balance(AccountType.debit, "Credti card", credit = credit)
        val repaymentDebtOperItem = MoneyOperItem.of(UUID.randomUUID(), balance, BigDecimal("100.00"), LocalDate.parse("2024-01-05"), 0)
        val operItem = MoneyOperItem.of(UUID.randomUUID(), balance, BigDecimal("-99.00"), LocalDate.parse("2023-12-01"), 0)
            .apply {
                this.repaymentSchedule = RepaymentSchedule.of(
                    RepaymentScheduleItem.of(LocalDate.parse("2023-12-01"), credit, BigDecimal("33.00"))!!,
                    RepaymentScheduleItem.of(LocalDate.parse("2024-01-01"), credit, BigDecimal("33.00"))!!,
                    RepaymentScheduleItem.of(LocalDate.parse("2024-02-01"), credit, BigDecimal("33.00"))!!
                )
            }

        val actualMainDebt = operItem.earlyRepaymentDebt(repaymentDebtOperItem, repaymentDebtOperItem.value)

        assertEquals(BigDecimal("67.00"), actualMainDebt)
    }
}