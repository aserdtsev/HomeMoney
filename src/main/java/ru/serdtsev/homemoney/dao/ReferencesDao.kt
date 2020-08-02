package ru.serdtsev.homemoney.dao

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import ru.serdtsev.homemoney.dto.HmCurrency
import java.util.*
import java.util.stream.Collectors
import javax.sql.DataSource

@Suppress("JoinDeclarationAndAssignment")
@Component
class ReferencesDao (dataSource: DataSource) {
    private val jdbcTemplate: JdbcTemplate

    fun getCurrencies(bsId: UUID): List<HmCurrency> {
        val sql = """
            select b.currency_code  from accounts a, balances b  
                where a.balance_sheet_id = ? and b.id = a.id  
                group by currency_code
            """.trimIndent()
        return jdbcTemplate.queryForList(sql, String::class.java, bsId)
                .stream()
                .map { code: String? ->
                    val currency = Currency.getInstance(code)
                    HmCurrency(currency.currencyCode, currency.displayName, currency.symbol)
                }
                .collect(Collectors.toList())
    }

    init {
        jdbcTemplate = JdbcTemplate(dataSource)
    }
}