package ru.serdtsev.homemoney.moneyoper.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import java.math.BigDecimal

internal class MoneyOperTest {
    private val balanceSheet = BalanceSheet()
    private val balance1 = Balance(balanceSheet, AccountType.debit, "Balance 1", BigDecimal("100.00"))
    private val balance2 = Balance(balanceSheet, AccountType.debit, "Balance 2", BigDecimal("200.00"))

    @Test
    fun merge() {
        val origTags = listOf(Tag(balanceSheet, "tag1"), Tag(balanceSheet, "tag2"))
        val origOper = MoneyOper(balanceSheet, MoneyOperStatus.done, tags = origTags, comment = "orig comment",
            period = Period.month)
        origOper.addItem(balance1, BigDecimal("-20.00"))

        val newTags = listOf(Tag(balanceSheet, "tag2"), Tag(balanceSheet, "tag3"))
        val newOper = MoneyOper(origOper.id, balanceSheet, status = MoneyOperStatus.done, tags = newTags,
            comment = "new comment", period = Period.single)
        newOper.addItem(balance1, BigDecimal("-30.00"))

        val actual = origOper.merge(newOper)

        assertThat(origOper)
            .extracting("comment", "period")
            .contains("new comment", Period.single)
        assertThat(origOper.items)
            .first()
            .extracting("balance", "value")
            .contains(balance1, BigDecimal("-30.00"))
        assertThat(origOper.tags).containsAll(newTags)
        assertThat(balance1).extracting("value").isEqualTo(BigDecimal("90.00"))
        assertThat(actual).containsAll(listOf(origOper, balance1))
    }

    @Test
    fun `merge changed balance`() {
        val origTags = listOf(Tag(balanceSheet, "tag1"), Tag(balanceSheet, "tag2"))
        val origOper = MoneyOper(balanceSheet, MoneyOperStatus.done, tags = origTags, comment = "orig comment",
            period = Period.month)
        origOper.addItem(balance1, BigDecimal("-20.00"))

        val newTags = listOf(Tag(balanceSheet, "tag2"), Tag(balanceSheet, "tag3"))
        val newOper = MoneyOper(origOper.id, balanceSheet, status = MoneyOperStatus.done, tags = newTags,
            comment = "new comment", period = Period.single)
        newOper.addItem(balance2, BigDecimal("-30.00"))

        val actual = origOper.merge(newOper)

        assertThat(origOper.items)
            .first()
            .extracting("balance", "value")
            .contains(balance2, BigDecimal("-30.00"))
        assertThat(balance1).extracting("value").isEqualTo(BigDecimal("120.00"))
        assertThat(balance2).extracting("value").isEqualTo(BigDecimal("170.00"))
        assertThat(actual).containsAll(listOf(origOper, balance1, balance2))
    }

    @Test
    fun merge_pendingToDone() {
        val origOper = MoneyOper(balanceSheet, MoneyOperStatus.pending)
        origOper.addItem(balance1, BigDecimal("-20.00"))

        val newOper = MoneyOper(origOper.id, balanceSheet, status = MoneyOperStatus.done)
        newOper.addItem(balance2, BigDecimal("-20.00"))

        origOper.merge(newOper)

        assertThat(origOper).extracting("status").isEqualTo(MoneyOperStatus.done)
    }
}