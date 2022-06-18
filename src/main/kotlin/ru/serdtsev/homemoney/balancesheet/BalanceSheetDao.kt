package ru.serdtsev.homemoney.balancesheet

import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*

@Service
class BalanceSheetDao(private val jdbcTemplate: NamedParameterJdbcTemplate) {
    fun save(balanceSheet: BalanceSheet) {
        val sql = """
            insert into balance_sheet(id, created_ts, currency_code) values (:id, :createdTs, :currencyCode)
            on conflict(id) do update set created_ts = :createdTs, currency_code = :currencyCode
        """.trimIndent()
        val paramMap = with(balanceSheet) {
            mapOf("id" to id, "createdTs" to Timestamp(createdTs.toEpochMilli()), "currencyCode" to currencyCode)
        }
        jdbcTemplate.update(sql, paramMap)
    }

    fun deleteById(id: UUID) {
        val sql = "delete from balance_sheet where id = :id"
        jdbcTemplate.update(sql, mapOf("id" to id))
    }

    fun exists(id: UUID): Boolean {
        val sql = "select 'x' from balance_sheet where id = :id"
        return jdbcTemplate.queryForList(sql, mapOf("id" to id), String::class.java).isNotEmpty()
    }

    @Cacheable("BalanceSheetDao.findById", keyGenerator = "")
    fun findById(id: UUID): BalanceSheet = findByIdOrNull(id)!!

    @Cacheable("BalanceSheetDao.findById", unless = "#result == null")
    fun findByIdOrNull(id: UUID): BalanceSheet? {
        val sql = "select * from balance_sheet where id = :id"
        return jdbcTemplate.query(sql, mapOf("id" to id)) { rs, _ ->
            val id1 = UUID.fromString(rs.getString("id"))
            val createdTs = rs.getTimestamp("created_ts").toInstant()
            val currencyCode = rs.getString("currency_code")
            BalanceSheet(id1, createdTs, currencyCode)
        }.firstOrNull()
    }

    fun getAggregateAccountSaldoList(id: UUID): List<Pair<AccountType, BigDecimal>> {
        val sql = """
            select type, sum(saldo) as saldo 
            from v_crnt_saldo_by_base_cry 
            where balance_sheet_id = :bsId 
            group by type
        """.trimIndent()
        val paramMap = mapOf("bsId" to id)
        return jdbcTemplate.query(sql, paramMap) { rs, _ ->
            val type = AccountType.valueOf(rs.getString("type"))
            val saldo = rs.getBigDecimal("saldo")
            Pair(type, saldo)
        }
    }

}