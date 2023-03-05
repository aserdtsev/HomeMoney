package ru.serdtsev.homemoney.moneyoper.dao

import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.serdtsev.homemoney.balancesheet.BalanceSheetDao
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus
import ru.serdtsev.homemoney.moneyoper.model.Period
import java.sql.ResultSet
import java.time.LocalDate
import java.util.*

@Repository
class MoneyOperDao(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val balanceSheetDao: BalanceSheetDao,
    private val moneyOperItemDao: MoneyOperItemDao,
    private val tagDao: TagDao
) {
    fun save(moneyOper: MoneyOper) {
        val sql = """
            insert into money_oper(id, balance_sheet_id, created_ts, trn_date, date_num, comment, period, status, recurrence_id)
               values(:id, :bsId, :createdTs, :performed, :dateNum, :comment, :period, :status, :recurrenceId)
            on conflict(id) do update set 
               balance_sheet_id = :bsId, created_ts = :createdTs, trn_date = :performed, date_num = :dateNum,
                comment = :comment, period = :period, status = :status, recurrence_id = :recurrenceId
            
        """.trimIndent()
        val paramMap = with(moneyOper) {
            mapOf("id" to id, "bsId" to balanceSheet.id, "createdTs" to created, "performed" to performed,
                "dateNum" to dateNum, "comment" to comment, "period" to period.toString(),
                "status" to status.toString(), "recurrenceId" to recurrenceId)
        }
        jdbcTemplate.update(sql,paramMap)

        moneyOperItemDao.deleteByMoneyOperId(moneyOper.id)
        moneyOperItemDao.save(moneyOper.items)

        tagDao.updateLinks(moneyOper.tags, moneyOper.id, "operation")
    }

    fun findById(id: UUID): MoneyOper = findByIdOrNull(id)!!

    fun findByIdOrNull(id: UUID): MoneyOper? {
        val sql = "select * from money_oper where id = :id"
        return jdbcTemplate.query(sql, mapOf("id" to id), rowMapper).firstOrNull()
    }

    fun exists(id: UUID): Boolean = findByIdOrNull(id) != null

    fun findByBalanceSheetAndStatus(balanceSheet: BalanceSheet, status: MoneyOperStatus,
            pageable: Pageable): Page<MoneyOper> {
        val whereCondition = "balance_sheet_id = :bsId and status = :status"
        val total = run {
            val sql = "select count(*) from money_oper where $whereCondition"
            val paramMap = mapOf("bsId" to balanceSheet.id, "status" to status.toString())
            jdbcTemplate.queryForObject(sql, paramMap, Long::class.java)!!
        }
        val orderByExpression = pageable.sort
            .map { it.property + if (it.isDescending) " desc" else "" }
            .joinToString(", ")
        val sql = """
            select * from money_oper 
            where $whereCondition 
            order by $orderByExpression 
            limit :limit 
            offset :offset""".trimIndent()
        val paramMap = mapOf("bsId" to balanceSheet.id, "status" to status.toString(), "limit" to pageable.pageSize,
            "offset" to pageable.offset)
        val list = jdbcTemplate.query(sql, paramMap, rowMapper)
        return PageImpl(list, pageable, total)
    }

    fun findByBalanceSheetAndStatusAndPerformed(balanceSheet: BalanceSheet, status: MoneyOperStatus, performed: LocalDate,
            pageable: Pageable): Page<MoneyOper> {
        val whereCondition = "balance_sheet_id = :bsId and status = :status and trn_date = :performed"
        val total = run {
            val sql = "select count(*) from money_oper where $whereCondition"
            val paramMap = mapOf("bsId" to balanceSheet.id, "status" to status.toString(), "performed" to performed)
            jdbcTemplate.queryForObject(sql, paramMap, Long::class.java)!!
        }
        val sql = "select * from money_oper where $whereCondition order by date_num limit :limit offset :offset"
        val paramMap = mapOf("bsId" to balanceSheet.id, "status" to status.toString(), "performed" to performed,
            "limit" to pageable.pageSize, "offset" to pageable.offset)
        val list = jdbcTemplate.query(sql, paramMap, rowMapper)
        return PageImpl(list, pageable, total)
    }

    fun findByBalanceSheetAndStatusAndPerformed(balanceSheet: BalanceSheet, status: MoneyOperStatus,
            performed: LocalDate): List<MoneyOper> {
        val sql = """
            select * from money_oper 
            where balance_sheet_id = :bsId and status = :status and trn_date = :performed 
            order by date_num 
        """.trimIndent()
        val paramMap = mapOf("bsId" to balanceSheet.id, "status" to status.toString(), "performed" to performed)
        return jdbcTemplate.query(sql, paramMap, rowMapper)
    }

    fun findByBalanceSheetAndStatusAndPerformedGreaterThan(balanceSheet: BalanceSheet, status: MoneyOperStatus,
        performed: LocalDate): List<MoneyOper> {
        val sql = """
            select * from money_oper 
            where balance_sheet_id = :bsId and status = :status and trn_date > :performed 
            order by date_num 
        """.trimIndent()
        val paramMap = mapOf("bsId" to balanceSheet.id, "status" to status.toString(), "performed" to performed)
        return jdbcTemplate.query(sql, paramMap, rowMapper)
    }

    private val rowMapper: (rs: ResultSet, rowNum: Int) -> MoneyOper = { rs, _ ->
        val id = UUID.fromString(rs.getString("id"))
        val balanceSheet = rs.getString("balance_sheet_id")
            .let { UUID.fromString(it) }
            .let { balanceSheetDao.findById(it) }
        val items = moneyOperItemDao.findByMoneyOperId(id).toMutableList()
        val status = rs.getString("status").let { MoneyOperStatus.valueOf(it) }
        val performed = rs.getDate("trn_date").toLocalDate()
        val dateNum = rs.getInt("date_num")
        val tags = tagDao.findByObjId(id)
        val comment = rs.getString("comment")
        val period = rs.getString("period")?.let { Period.valueOf(it) }
        MoneyOper(id, balanceSheet, items, status, performed, dateNum, tags, comment, period).apply {
            this.created = rs.getTimestamp("created_ts")
            this.recurrenceId = rs.getString("recurrence_id")?.let { UUID.fromString(it) }
        }
    }
}