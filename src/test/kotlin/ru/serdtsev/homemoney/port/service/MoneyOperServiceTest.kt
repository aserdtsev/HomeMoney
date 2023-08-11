package ru.serdtsev.homemoney.port.service

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.domain.model.account.Account
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.domain.repository.BalanceRepository
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.domain.repository.TagRepository
import ru.serdtsev.homemoney.infra.dao.AccountDao
import ru.serdtsev.homemoney.infra.dao.BalanceSheetDao
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

internal class MoneyOperServiceTest {
    private val balanceSheetDao: BalanceSheetDao = mock {  }
    private val tagRepository: TagRepository = mock {  }
    private val moneyOperRepository: MoneyOperRepository = mock {  }
    private val balanceSheet = BalanceSheet()
    private val accountDao: AccountDao = mock {  }
    private val service = MoneyOperService(moneyOperRepository, mock {  }, tagRepository)

    @BeforeEach
    fun setUp() {
        whenever(balanceSheetDao.findById(any())).thenReturn(balanceSheet)
    }

    @Test
    fun getTagsSuggest() {
        val car = newTag("car")
        val food = newTag("food")
        val clothes = newTag("clothes")
        val tagsA = listOf(car, food, clothes)
        val tagsB = listOf(food, clothes)
        val tagsC = listOf(clothes)

        val opers = listOf(newMoneyOperWithTags(tagsA), newMoneyOperWithTags(tagsB), newMoneyOperWithTags(tagsC))
        whenever(moneyOperRepository.findByBalanceSheetAndStatusAndPerformedGreaterThan(any(), any()))
            .thenReturn(opers)

        whenever(tagRepository.findByBalanceSheetOrderByName())
            .thenReturn(tagsA)

        val account = Account(UUID.randomUUID(), AccountType.debit,"Some account name",
            LocalDate.now(),false)
        whenever(accountDao.findNameById(any())).thenReturn(account.name)

        val moneyOper = newMoneyOperWithTags(ArrayList())
        val tags = moneyOper.tags.map { it.name }
        val actual = service.getSuggestTags(MoneyOperType.expense.name, null, tags)
        val expectedTags = listOf(car, food, clothes)
        assertIterableEquals(expectedTags, actual)
    }

    private fun newMoneyOperWithTags(tags: List<Tag>): MoneyOper {
        val moneyOper = MoneyOper(UUID.randomUUID(), mutableListOf(), MoneyOperStatus.doneNew,
            LocalDate.now(), 0, tags, null, Period.month)
        val balance1 = Balance(AccountType.debit, "Some account name", BigDecimal.ZERO)
        val balance2 = Balance(AccountType.debit, "Some account name", BigDecimal.ZERO)
        moneyOper.addItem(balance1, BigDecimal.valueOf(1).negate(), LocalDate.now(), 0, UUID.randomUUID())
        moneyOper.addItem(balance2, BigDecimal.ONE, LocalDate.now(), 1, UUID.randomUUID())
        return moneyOper
    }

    private fun newTag(name: String): Tag {
        return Tag(name).apply {
            this.isCategory = true
            this.categoryType = CategoryType.expense
        }
    }
}