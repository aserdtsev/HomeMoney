package ru.serdtsev.homemoney.port.service

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test
import ru.serdtsev.homemoney.SpringBootBaseTest
import ru.serdtsev.homemoney.domain.model.account.Account
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.moneyoper.*
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.domain.repository.TagRepository
import ru.serdtsev.homemoney.port.dao.AccountDao
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

internal class TagServiceTest : SpringBootBaseTest() {
    private val tagRepository: TagRepository = mock {  }
    private val moneyOperRepository: MoneyOperRepository = mock {  }
    private val accountDao: AccountDao = mock {  }
    private val service = TagService(tagRepository, moneyOperRepository)

    @Test
    fun getTagsSuggest() {
        val car = createExpenseTag("car")
        val food = createExpenseTag("food")
        val clothes = createExpenseTag("clothes")
        val tagsA = listOf(car, food, clothes)
        val tagsB = listOf(food, clothes)
        val tagsC = listOf(clothes)

        val opers = listOf(createMoneyOperWithTags(tagsA), createMoneyOperWithTags(tagsB), createMoneyOperWithTags(tagsC))
        whenever(moneyOperRepository.findByStatusAndPerformedGreaterThan(any(), any()))
            .thenReturn(opers)

        whenever(tagRepository.findByBalanceSheetOrderByName())
            .thenReturn(tagsA)

        val account = Account(UUID.randomUUID(), AccountType.debit,"Some account name",
            LocalDate.now(),false)
        whenever(accountDao.findNameById(any())).thenReturn(account.name)

        val moneyOper = createMoneyOperWithTags(ArrayList())
        val tags = moneyOper.tags.map { it.name }
        val actual = service.getSuggestTags(MoneyOperType.expense.name, null, tags)
        val expectedTags = listOf(car, food, clothes)
        assertIterableEquals(expectedTags, actual)
    }

    private fun createMoneyOperWithTags(tags: List<Tag>): MoneyOper {
        val moneyOper = MoneyOper(UUID.randomUUID(), mutableListOf(), MoneyOperStatus.Done,
            LocalDate.now(), tags, null, Period.Month, dateNum = 0)
        val balance1 = Balance(AccountType.debit, "Some account name", BigDecimal.ZERO)
        val balance2 = Balance(AccountType.debit, "Some account name", BigDecimal.ZERO)
        moneyOper.addItem(balance1, BigDecimal.valueOf(1).negate(), LocalDate.now(), id = UUID.randomUUID())
        moneyOper.addItem(balance2, BigDecimal.ONE, LocalDate.now(), id = UUID.randomUUID())
        return moneyOper
    }

    private fun createExpenseTag(name: String): Tag = Tag.of(name, CategoryType.expense)
}