package ru.serdtsev.homemoney.infra.dao

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.model.moneyoper.RecurrenceOper
import ru.serdtsev.homemoney.domain.repository.RecurrenceOperRepository
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import java.sql.ResultSet
import java.util.*

@Repository
class RecurrenceOperDao(
    private val jdbcTemplate: NamedParameterJdbcTemplate
): DomainModelDao<RecurrenceOper>, RecurrenceOperRepository {
    override fun save(domainAggregate: RecurrenceOper) {
        val sql = """
            insert into recurrence_oper(id, template_id, next_date, balance_sheet_id, is_arc)
                values(:id, :templateId, :nextDate, :bsId, :isArc)
            on conflict(id) do update set
                template_id = :templateId, next_date = :nextDate, balance_sheet_id = :bsId, is_arc = :isArc
        """.trimIndent()
        val paramMap = with(domainAggregate) {
            mapOf("id" to id, "templateId" to templateId, "nextDate" to nextDate,
                "bsId" to ApiRequestContextHolder.balanceSheet.id, "isArc" to arc)
        }
        jdbcTemplate.update(sql, paramMap)
    }

    override fun findById(id: UUID): RecurrenceOper = findByIdOrNull(id)!!

    override fun findByIdOrNull(id: UUID): RecurrenceOper? {
        val sql = "select * from recurrence_oper where id = :id"
        return jdbcTemplate.query(sql, mapOf("id" to id), rowMapper).firstOrNull()
    }

    override fun exists(id: UUID): Boolean = findByIdOrNull(id) != null

    override fun findByBalanceSheetAndArc(balanceSheet: BalanceSheet, isArc: Boolean?): List<RecurrenceOper> {
        val sql = "select * from recurrence_oper where balance_sheet_id = :bsId " +
                if (isArc != null) "and is_arc = :isArc" else ""
        val paramMap = mapOf("bsId" to balanceSheet.id, "isArc" to isArc)
        return jdbcTemplate.query(sql, paramMap, rowMapper)
    }

    private val rowMapper: (rs: ResultSet, rowNum: Int) -> RecurrenceOper = { rs, _ ->
        val id = rs.getString("id").let { UUID.fromString(it) }
        val templateId = rs.getString("template_id")
            .let { UUID.fromString(it) }
        val nextDate = rs.getDate("next_date").toLocalDate()
        val isArc = rs.getBoolean("is_arc")
        RecurrenceOper(id, templateId, nextDate).apply { this.arc = isArc }
    }
}