package ru.serdtsev.homemoney.moneyoper.dao

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.serdtsev.homemoney.balancesheet.BalanceSheetDao
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet
import ru.serdtsev.homemoney.moneyoper.model.RecurrenceOper
import java.sql.ResultSet
import java.util.*

@Repository
class RecurrenceOperDao(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val balanceSheetDao: BalanceSheetDao,
    private val moneyOperDao: MoneyOperDao
) {
    fun save(recurrenceOper: RecurrenceOper) {
        val sql = """
            insert into recurrence_oper(id, template_id, next_date, balance_sheet_id, is_arc)
                values(:id, :templateId, :nextDate, :bsId, :isArc)
            on conflict(id) do update set
                template_id = :templateId, next_date = :nextDate, balance_sheet_id = :bsId, is_arc = :isArc
        """.trimIndent()
        val paramMap = with(recurrenceOper) {
            mapOf("id" to id, "templateId" to template.id, "nextDate" to nextDate, "bsId" to balanceSheet.id, "isArc" to arc)
        }
        jdbcTemplate.update(sql, paramMap)
    }

    fun findById(id: UUID): RecurrenceOper = findByIdOrNull(id)!!

    fun findByIdOrNull(id: UUID): RecurrenceOper? {
        val sql = "select * from recurrence_oper where id = :id"
        return jdbcTemplate.query(sql, mapOf("id" to id), rowMapper).firstOrNull()
    }

    fun exists(id: UUID): Boolean = findByIdOrNull(id) != null

    fun findByBalanceSheetAndArc(balanceSheet: BalanceSheet, isArc: Boolean? = null): List<RecurrenceOper> {
        val sql = "select * from recurrence_oper where balance_sheet_id = :bsId " +
                if (isArc != null) "and is_arc = :isArc" else ""
        val paramMap = mapOf("bsId" to balanceSheet.id, "isArc" to isArc)
        return jdbcTemplate.query(sql, paramMap, rowMapper)
    }

    private val rowMapper: (rs: ResultSet, rowNum: Int) -> RecurrenceOper = { rs, _ ->
        val id = rs.getString("id").let { UUID.fromString(it) }
        val balanceSheet = rs.getString("balance_sheet_id")
            .let { UUID.fromString(it) }
            .let { balanceSheetDao.findById(it) }
        val template = rs.getString("template_id")
            .let { UUID.fromString(it) }
            .let { moneyOperDao.findById(it) }
        val nextDate = rs.getDate("next_date").toLocalDate()
        val isArc = rs.getBoolean("is_arc")
        RecurrenceOper(id, balanceSheet, template, nextDate).apply { this.arc = isArc }
    }
}