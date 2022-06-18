package ru.serdtsev.homemoney.account.dao

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.account.model.Reserve
import ru.serdtsev.homemoney.balancesheet.BalanceSheetDao
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import java.sql.ResultSet
import java.util.*

@Service
class ReserveDao(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val balanceSheetDao: BalanceSheetDao
) {

    fun save(reserve: Reserve) {
        val id = reserve.id
        val sql = """
            insert into account(id, balance_sheet_id, name, created_date, type, is_arc) 
                values (:id, :bsId, :name, :createdDate, :type, :isArc)
                on conflict(id) do update set name = :name, created_date = :createdDate, type = :type, is_arc = :isArc;
            insert into balance(id, currency_code, value, num)
                values (:id, :currencyCode, :value, :num)
                on conflict(id) do update set currency_code = :currencyCode, value = :value, num = :num;
            insert into reserve(id, target) values (:id, :target)
                on conflict(id) do update set target = :target;    
        """.trimIndent()
        val paramMap = with(reserve) {
            mapOf(
                "id" to id, "bsId" to balanceSheet.id, "name" to name, "createdDate" to createdDate,
                "type" to type.toString(), "isArc" to isArc, "currencyCode" to currencyCode, "value" to value,
                "target" to target, "num" to num
            )
        }
        jdbcTemplate.update(sql, paramMap)
    }

    fun delete(reserve: Reserve) {
        val sql = """
            delete from reserve where id = :id;
            delete from balance where id = :id;
            delete from account where id = :id;
        """.trimIndent()
        val paramMap = mapOf("id" to reserve.id)
        jdbcTemplate.update(sql, paramMap)
    }

    fun exists(id: UUID): Boolean = findByIdOrNull(id) != null

    fun findById(id: UUID): Reserve = findByIdOrNull(id)!!

    fun findByIdOrNull(id: UUID): Reserve? {
        val sql = """
            select a.id, a.balance_sheet_id, a.name, a.created_date, a.type, a.is_arc, 
                b.currency_code, b.value, b.num, r.target
            from account a, balance b, reserve r
            where a.id = :id and b.id = a.id and r.id = a.id
        """.trimIndent()

        return jdbcTemplate.query(sql, mapOf("id" to id), rowMapper).firstOrNull()
    }

    fun findByBalanceSheet(balanceSheet: BalanceSheet): List<Reserve> {
        val sql = """
            select a.id, a.balance_sheet_id, a.name, a.created_date, a.type, a.is_arc, 
                b.currency_code, b.value, b.num, r.target
            from account a, balance b, reserve r
            where a.balance_sheet_id = :bsId and b.id = a.id and r.id = a.id
        """.trimIndent()
        val paramMap = mapOf("bsId" to balanceSheet.id)
        return jdbcTemplate.query(sql, paramMap, rowMapper)
    }

    private val rowMapper: (rs: ResultSet, rowNum: Int) -> Reserve = { rs, _ ->
        val id1 = UUID.fromString(rs.getString("id"))
        val balanceSheetId = UUID.fromString(rs.getString("balance_sheet_id"))
        val balanceSheet = balanceSheetDao.findById(balanceSheetId)
        val createdDate = rs.getDate("created_date").toLocalDate()
        val name = rs.getString("name")
        val currencyCode = rs.getString("currency_code")
        val value = rs.getBigDecimal("value")
        val target = rs.getBigDecimal("target")
        Reserve(id1, balanceSheet, name, createdDate, currencyCode, value, target).apply {
            this.isArc = rs.getBoolean("is_arc")
            this.num = rs.getLong("num")
        }
    }

}