package ru.serdtsev.homemoney.infra.dao

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.serdtsev.homemoney.domain.event.DomainEventPublisher
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.model.moneyoper.RecurrenceOper
import ru.serdtsev.homemoney.domain.repository.BalanceSheetRepository
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.domain.repository.RecurrenceOperRepository
import java.sql.ResultSet
import java.util.*

@Repository
class RecurrenceOperDao(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val balanceSheetRepository: BalanceSheetRepository,
    private val moneyOperRepository: MoneyOperRepository
): DomainModelDao<RecurrenceOper>, RecurrenceOperRepository {
    override fun save(domainAggregate: RecurrenceOper) {
        val sql = """
            insert into recurrence_oper(id, template_id, next_date, balance_sheet_id, is_arc)
                values(:id, :templateId, :nextDate, :bsId, :isArc)
            on conflict(id) do update set
                template_id = :templateId, next_date = :nextDate, balance_sheet_id = :bsId, is_arc = :isArc
        """.trimIndent()
        val paramMap = with(domainAggregate) {
            mapOf("id" to id, "templateId" to template.id, "nextDate" to nextDate, "bsId" to balanceSheet.id, "isArc" to arc)
        }
        jdbcTemplate.update(sql, paramMap)
        DomainEventPublisher.instance.publish(domainAggregate.template)
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
        val balanceSheet = rs.getString("balance_sheet_id")
            .let { UUID.fromString(it) }
            .let { balanceSheetRepository.findById(it) }
        val template = rs.getString("template_id")
            .let { UUID.fromString(it) }
            .let { moneyOperRepository.findById(it) }
        val nextDate = rs.getDate("next_date").toLocalDate()
        val isArc = rs.getBoolean("is_arc")
        RecurrenceOper(id, balanceSheet, template, nextDate).apply { this.arc = isArc }
    }
}