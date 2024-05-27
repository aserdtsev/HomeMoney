package ru.serdtsev.homemoney.port.dao

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.serdtsev.homemoney.domain.model.HmCurrency
import ru.serdtsev.homemoney.domain.repository.ReferenceRepository
import java.util.*

@Repository
class ReferenceDao (private val jdbcTemplate: NamedParameterJdbcTemplate) : ReferenceRepository {
    override fun getCurrencies(bsId: UUID): List<HmCurrency> {
        val sql = """
            select b.currency_code  from account a, balance b  
            where a.balance_sheet_id = :bsId and b.id = a.id  
            group by currency_code
            """.trimIndent()
        return jdbcTemplate.queryForList(sql, mapOf("bsId" to bsId), String::class.java)
                .map { code: String? ->
                    val currency = Currency.getInstance(code)
                    HmCurrency(currency.currencyCode, currency.displayName, currency.symbol)
                }
    }
}