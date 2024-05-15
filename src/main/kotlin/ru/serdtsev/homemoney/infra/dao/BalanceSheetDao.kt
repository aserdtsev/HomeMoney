package ru.serdtsev.homemoney.infra.dao

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.repository.BalanceSheetRepository
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*

@Repository
class BalanceSheetDao(private val jdbcTemplate: NamedParameterJdbcTemplate) : DomainModelDao<BalanceSheet>,
    BalanceSheetRepository {
    override fun save(domainAggregate: BalanceSheet) {
        val sql = """
            insert into balance_sheet(id, created_ts, currency_code) values (:id, :createdTs, :currencyCode)
            on conflict(id) do update set created_ts = :createdTs, currency_code = :currencyCode
        """.trimIndent()
        val paramMap = with(domainAggregate) {
            mapOf("id" to id, "createdTs" to Timestamp(createdTs.toEpochMilli()), "currencyCode" to currencyCode)
        }
        jdbcTemplate.update(sql, paramMap)
    }

    @CacheEvict("BalanceSheet")
    override fun deleteById(id: UUID) {
        val sql = "delete from balance_sheet where id = :id"
        jdbcTemplate.update(sql, mapOf("id" to id))
    }

    override fun exists(id: UUID): Boolean {
        val sql = "select 'x' from balance_sheet where id = :id"
        return jdbcTemplate.queryForList(sql, mapOf("id" to id), String::class.java).isNotEmpty()
    }

    @Cacheable("BalanceSheet", keyGenerator = "")
    override fun findById(id: UUID): BalanceSheet = findByIdOrNull(id)!!

    @Cacheable("BalanceSheet", condition = "#result != null")
    override fun findByIdOrNull(id: UUID): BalanceSheet? {
        val sql = "select * from balance_sheet where id = :id"
        return jdbcTemplate.query(sql, mapOf("id" to id), rowMapper).firstOrNull()
    }

    override fun getAggregateAccountSaldoList(): List<Pair<AccountType, BigDecimal>> {
        val id = ApiRequestContextHolder.instance.getBsId()
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

    override fun getActualDebt(): BigDecimal {
        val id = ApiRequestContextHolder.instance.getBsId()
        val sql = """
            select sum(round(coalesce((b.credit -> 'annuityPayment' -> 'value')::numeric * -1, b.value) * coalesce(er.ask, 1), 2)) as value
            from account a
                join balance b on b.id = a.id
                left join exchange_rate er
                  on substring(er.id from 1 for 3) = b.currency_code
                     and date = (select max(date) from exchange_rate er1 where substring(er1.id from 1 for 3) = b.currency_code)
            where a.type = 'credit' and a.balance_sheet_id = :bsId            
        """.trimIndent()
        val paramMap = mapOf("bsId" to id)
        return jdbcTemplate.queryForObject(sql, paramMap, BigDecimal::class.java) ?: BigDecimal.ZERO
    }

    override fun findAll(): List<BalanceSheet> = jdbcTemplate.query("select * from balance_sheet", rowMapper)

    private val rowMapper: (rs: ResultSet, rowNum: Int) -> BalanceSheet = { rs, _ ->
        val id = UUID.fromString(rs.getString("id"))
        val createdTs = rs.getTimestamp("created_ts").toInstant()
        val currencyCode = rs.getString("currency_code")
        BalanceSheet(id, createdTs, currencyCode)
    }
}