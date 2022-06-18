package ru.serdtsev.homemoney.moneyoper.dao

import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import ru.serdtsev.homemoney.account.dao.BalanceDao
import ru.serdtsev.homemoney.account.model.Balance
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperItem
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import java.math.BigDecimal
import java.sql.ResultSet
import java.time.LocalDate
import java.util.*

@Service
class MoneyOperItemDao(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val balanceDao: BalanceDao
) {
    fun save(moneyOperItems: List<MoneyOperItem>) {
        moneyOperItems.forEach { save(it) }
    }

    fun save(moneyOperItem: MoneyOperItem) {
        val sql = """
            insert into money_oper_item(id, oper_id, balance_id, value, performed, index, bs_id)
                values(:id, :operId, :balanceId, :value, :performed, :index, :bsId)
            on conflict(id) do update set  
                oper_id = :operId, balance_id = :balanceId, value = :value, performed = :performed, index = :index,
                bs_id = :bsId
        """.trimIndent()
        val paramMap = with(moneyOperItem) {
            mapOf("id" to id, "operId" to moneyOperId, "balanceId" to balance.id, "value" to value,
                "performed" to performed, "index" to index, "bsId" to  balanceSheet.id)
        }
        jdbcTemplate.update(sql, paramMap)
    }

    fun deleteByMoneyOperId(moneyOperId: UUID) {
        val sql = "delete from money_oper_item where oper_id = :operId"
        jdbcTemplate.update(sql, mapOf("operId" to moneyOperId))
    }

    fun findById(id: UUID) = findByIdOrNull(id)!!

    fun findByIdOrNull(id: UUID): MoneyOperItem? {
        val sql = "select * from money_oper_item where id = :id"
        return jdbcTemplate.query(sql, mapOf("id" to id), rowMapper).firstOrNull()
    }

    fun exists(id: UUID): Boolean = findByIdOrNull(id) != null

    fun findByBalanceSheetAndValueOrderByPerformedDesc(balanceSheet: BalanceSheet, absValue: BigDecimal,
        pageable: Pageable): Page<MoneyOperItem> {
        val total = run {
            val sql = "select count(*) from money_oper_item where bs_id = :bsId and abs(value) = :value"
            val paramMap = mapOf("bsId" to balanceSheet.id, "value" to absValue)
            jdbcTemplate.queryForObject(sql, paramMap, Long::class.java)!!
        }
        val sql = """
            select * from money_oper_item 
            where bs_id = :bsId and abs(value) = :value
            order by performed desc
            limit :limit
            offset :offset    
        """.trimIndent()
        val paramMap = mapOf("bsId" to balanceSheet.id, "value" to absValue, "limit" to pageable.pageSize,
            "offset" to pageable.offset)
        val list = jdbcTemplate.query(sql, paramMap, rowMapper)
        return PageImpl(list, pageable, total)
    }

    fun findByBalanceSheetAndPerformedBetweenAndMoneyOperStatus(balanceSheet: BalanceSheet, startDate: LocalDate,
        finishDate: LocalDate, status: MoneyOperStatus): List<MoneyOperItem> {
        val sql = """
            select i.* from money_oper_item i, money_oper o  
            where i.bs_id = :bsId and i.performed between :startDate and :finishDate 
                and o.id = i.oper_id and o.status = :status
        """.trimIndent()
        val paramMap = mapOf("bsId" to balanceSheet.id, "startDate" to startDate, "finishDate" to finishDate,
            "status" to status.toString())
        return jdbcTemplate.query(sql, paramMap, rowMapper)
    }

    fun findByBalance(balance: Balance): List<MoneyOperItem> {
        val sql = "select * from money_oper_item where balance_id = :balanceId"
        return jdbcTemplate.query(sql, mapOf("balanceId" to balance.id), rowMapper)
    }

    @Cacheable("MoneyOperItemDao.findByMoneyOperId")
    fun findByMoneyOperId(moneyOperId: UUID): List<MoneyOperItem> {
        val sql = "select * from money_oper_item where oper_id = :operId order by index"
        return jdbcTemplate.query(sql, mapOf("operId" to moneyOperId), rowMapper)
    }

    private val rowMapper: (rs: ResultSet, rowNum: Int) -> MoneyOperItem = { rs, _ ->
        val id = UUID.fromString(rs.getString("id"))
        val moneyOperId = UUID.fromString(rs.getString("oper_id"))
        val balance = UUID.fromString(rs.getString("balance_id")).let { balanceDao.findById(it) }
        val value = rs.getBigDecimal("value")
        val performed = rs.getDate("performed").toLocalDate()
        val index = rs.getInt("index")
        MoneyOperItem(id, moneyOperId, balance, value, performed, index)
    }
}