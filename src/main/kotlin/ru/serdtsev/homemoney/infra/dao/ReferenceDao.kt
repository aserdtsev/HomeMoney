package ru.serdtsev.homemoney.infra.dao

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import ru.serdtsev.homemoney.domain.model.HmCurrency
import ru.serdtsev.homemoney.domain.repository.ReferenceRepository
import java.util.*
import java.util.stream.Collectors
import javax.sql.DataSource

@Repository
class ReferenceDao (dataSource: DataSource) : ReferenceRepository {
    private val jdbcTemplate: JdbcTemplate

    override fun getCurrencies(bsId: UUID): List<HmCurrency> {
        val sql = """
            select b.currency_code  from account a, balance b  
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