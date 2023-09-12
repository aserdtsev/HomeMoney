package ru.serdtsev.homemoney.infra.dao

import com.google.gson.Gson
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.serdtsev.homemoney.domain.model.account.AccountType
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.account.Credit
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.repository.BalanceRepository
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import ru.serdtsev.homemoney.infra.utils.toJsonb
import java.sql.ResultSet
import java.util.*

@Repository
class BalanceDao(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val reserveDao: ReserveDao,
    @Qualifier("firstLevelCacheManager") private val cacheManager: CacheManager
) : DomainModelDao<Balance>, BalanceRepository {
    private val log = KotlinLogging.logger {  }
    private val gson = Gson()

    @CacheEvict("Balance", key = "#domainAggregate.id", cacheManager = "firstLevelCacheManager")
    override fun save(domainAggregate: Balance) {
        val sql = """
            insert into account(id, balance_sheet_id, name, created_date, type, is_arc) 
                values (:id, :bsId, :name, :createdDate, :type, :isArc)
                on conflict(id) do update set
                    name = :name, created_date = :createdDate, type = :type, is_arc = :isArc;
            insert into balance(id, currency_code, value, min_value, reserve_id, num, credit)
                values (:id, :currencyCode, :value, :minValue, :reserveId, :num, :credit)
                on conflict(id) do update set
                    currency_code = :currencyCode, value = :value, min_value = :minValue, reserve_id = :reserveId, 
                    num = :num, credit = :credit;
        """.trimIndent()
        val paramMap = with(domainAggregate) {
            mapOf("id" to domainAggregate.id, "bsId" to ApiRequestContextHolder.balanceSheet.id, "name" to name,
                "createdDate" to createdDate, "type" to type.toString(), "isArc" to isArc, "currencyCode" to currencyCode,
                "value" to value, "minValue" to minValue, "reserveId" to reserveId, "num" to num,
                "credit" to credit?.let { gson.toJsonb(it) }
            )
        }
        jdbcTemplate.update(sql, paramMap)
    }

    @CacheEvict("Balance", key = "#balance.id", cacheManager = "firstLevelCacheManager")
    override fun delete(balance: Balance) {
        val sql = """
            delete from balance where id = :id;
            delete from account where id = :id;
        """.trimIndent()
        jdbcTemplate.update(sql, mapOf("id" to balance.id))
    }

    override fun exists(id: UUID): Boolean = findByIdOrNull(id) != null

    @Cacheable("Balance", cacheManager = "firstLevelCacheManager")
    override fun findById(id: UUID): Balance = findByIdOrNull(id)!!

    @Cacheable("Balance", cacheManager = "firstLevelCacheManager")
    override fun findByIdOrNull(id: UUID): Balance? {
        val sql = """
            select a.id, a.balance_sheet_id, a.name, a.created_date, a.type, a.is_arc,
                b.currency_code, b.value, b.credit, b.min_value, b.reserve_id, b.num, b.credit
            from account a, balance b
            where a.id = :id and b.id = a.id
        """.trimIndent()
        return jdbcTemplate.query(sql, mapOf("id" to id), rowMapper).firstOrNull()
    }

    override fun findByBalanceSheet(balanceSheet: BalanceSheet): List<Balance> {
        val sql = """
            select a.id, a.balance_sheet_id, a.name, a.created_date, a.type, a.is_arc,
                b.currency_code, b.value, b.credit, b.min_value, b.reserve_id, b.num, b.credit
            from account a, balance b
            where a.balance_sheet_id = :bsId and b.id = a.id and a.type in (:types)
            """.trimIndent()
        val types = AccountType.values().filter { it.isBalance }.map { it.name }
        val paramMap = mapOf("bsId" to balanceSheet.id, "types" to types)
        return jdbcTemplate.query(sql, paramMap, rowMapper)
    }

    private val rowMapper: (rs: ResultSet, rowNum: Int) -> Balance = { rs, _ ->
        val id = UUID.fromString(rs.getString("id"))
        cacheManager.getCache("Balance")?.get(id, Balance::class.java) ?: run {
            val type = AccountType.valueOf(rs.getString("type"))
            if (type == AccountType.reserve) {
                reserveDao.findById(id)
            } else {
                val createdDate = rs.getDate("created_date").toLocalDate()
                val name = rs.getString("name")
                val isArc = rs.getBoolean("is_arc")
                val value = rs.getBigDecimal("value")
                val currencyCode = rs.getString("currency_code")
                Balance(id, type, name, createdDate, isArc, currencyCode, value).apply {
                    this.minValue = rs.getBigDecimal("min_value")
                    this.credit = rs.getString("credit")
                        ?.let { gson.fromJson(it, Credit::class.java) }
                    this.reserve = rs.getString("reserve_id")
                        ?.let { UUID.fromString(it) }
                        ?.let { reserveDao.findByIdOrNull(it) }
                    this.num = rs.getLong("num")
                }
            }
        }
    }

}