package ru.serdtsev.homemoney.account.dao

import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.account.model.AccountType
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.BalanceSheetDao
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import java.sql.ResultSet
import java.util.*

@Service
class BalanceDao(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val balanceSheetDao: BalanceSheetDao,
    private val reserveDao: ReserveDao
) {
    fun save(balance: Balance) {
        val id = balance.id
        val sql = """
            insert into account(id, balance_sheet_id, name, created_date, type, is_arc) 
                values (:id, :bsId, :name, :createdDate, :type, :isArc)
                on conflict(id) do update set
                    name = :name, created_date = :createdDate, type = :type, is_arc = :isArc;
            insert into balance(id, currency_code, value, credit_limit, min_value, reserve_id, num)
                values (:id, :currencyCode, :value, :creditLimit, :minValue, :reserveId, :num)
                on conflict(id) do update set
                    currency_code = :currencyCode, value = :value, credit_limit = :creditLimit,
                    min_value = :minValue, reserve_id = :reserveId, num = :num;
        """.trimIndent()
        val paramMap = with(balance) {
            mapOf("id" to id, "bsId" to balance.balanceSheet.id, "name" to name, "createdDate" to createdDate,
                "type" to type.toString(), "isArc" to isArc, "currencyCode" to currencyCode, "value" to value,
                "creditLimit" to creditLimit, "minValue" to minValue, "reserveId" to reserveId, "num" to num
            )
        }
        jdbcTemplate.update(sql, paramMap)
    }

    fun delete(balance: Balance) {
        val sql = """
            delete from balance where id = :id;
            delete from account where id = :id;
        """.trimIndent()
        jdbcTemplate.update(sql, mapOf("id" to balance.id))
    }

    fun exists(id: UUID): Boolean = findByIdOrNull(id) != null

    @Cacheable("BalanceDao.findById")
    fun findById(id: UUID): Balance = findByIdOrNull(id)!!

    @Cacheable("BalanceDao.findById", unless = "#result == null")
    fun findByIdOrNull(id: UUID): Balance? {
        val sql = """
            select a.id, a.balance_sheet_id, a.name, a.created_date, a.type, a.is_arc, 
                b.currency_code, b.value, b.credit_limit, b.min_value, b.reserve_id, b.num
            from account a, balance b
            where a.id = :id and b.id = a.id
        """.trimIndent()
        return jdbcTemplate.query(sql, mapOf("id" to id), rowMapper).firstOrNull()
    }

    fun findByBalanceSheet(balanceSheet: BalanceSheet): List<Balance> {
        val sql = """
            select a.id, a.balance_sheet_id, a.name, a.created_date, a.type, a.is_arc, 
                b.currency_code, b.value, b.credit_limit, b.min_value, b.reserve_id, b.num
            from account a, balance b
            where a.balance_sheet_id = :bsId and b.id = a.id and a.type in (:types)
        """.trimIndent()
        val types = AccountType.values().filter { it.isBalance }.map { it.name }
        val paramMap = mapOf("bsId" to balanceSheet.id, "types" to types)
        return jdbcTemplate.query(sql, paramMap, rowMapper)
    }

    private val rowMapper: (rs: ResultSet, rowNum: Int) -> Balance = { rs, _ ->
        val id = UUID.fromString(rs.getString("id"))
        val balanceSheet = rs.getString("balance_sheet_id")
            .let { UUID.fromString(it) }
            .let { balanceSheetDao.findById(it) }
        val type = AccountType.valueOf(rs.getString("type"))
        val createdDate = rs.getDate("created_date").toLocalDate()
        val name = rs.getString("name")
        val isArc = rs.getBoolean("is_arc")
        val value = rs.getBigDecimal("value")
        val currencyCode = rs.getString("currency_code")
        Balance(id, balanceSheet, type, name, createdDate, isArc, currencyCode, value).apply {
            this.minValue = rs.getBigDecimal("min_value")
            this.creditLimit = rs.getBigDecimal("credit_limit")
            this.reserve = rs.getString("reserve_id")
                ?.let { UUID.fromString(it) }
                ?.let { reserveDao.findByIdOrNull(it) }
            this.num = rs.getLong("num")
        }
    }

}