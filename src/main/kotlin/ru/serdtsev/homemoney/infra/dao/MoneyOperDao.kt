package ru.serdtsev.homemoney.infra.dao

import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.GsonBuilder
import org.springframework.cache.annotation.CacheEvict
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.serdtsev.homemoney.domain.model.account.Balance
import ru.serdtsev.homemoney.domain.model.balancesheet.BalanceSheet
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOper
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperItem
import ru.serdtsev.homemoney.domain.model.moneyoper.MoneyOperStatus
import ru.serdtsev.homemoney.domain.model.moneyoper.Period
import ru.serdtsev.homemoney.domain.repository.MoneyOperRepository
import ru.serdtsev.homemoney.infra.ApiRequestContextHolder
import ru.serdtsev.homemoney.infra.utils.toJsonb
import java.math.BigDecimal
import java.sql.ResultSet
import java.time.LocalDate
import java.util.*


@Repository
class MoneyOperDao(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val tagDao: TagDao,
    private val balanceDao: BalanceDao
) : DomainModelDao<MoneyOper>, MoneyOperRepository {
    private val gson = Converters.registerAll(GsonBuilder()).create()

    @CacheEvict(cacheNames = ["TagDao.findByObjId"], key = "#domainAggregate.id")
    override fun save(domainAggregate: MoneyOper) {
        val sql = """
            insert into money_oper(id, balance_sheet_id, created_ts, trn_date, date_num, comment, period, status, recurrence_id)
               values(:id, :bsId, :createdTs, :performed, :dateNum, :comment, :period, :status, :recurrenceId)
            on conflict(id) do update set 
               balance_sheet_id = :bsId, created_ts = :createdTs, trn_date = :performed, date_num = :dateNum,
                comment = :comment, period = :period, status = :status, recurrence_id = :recurrenceId
            
        """.trimIndent()
        val paramMap = with(domainAggregate) {
            mapOf("id" to id, "bsId" to ApiRequestContextHolder.balanceSheet.id, "createdTs" to created,
                "performed" to performed, "dateNum" to dateNum, "comment" to comment, "period" to period.toString(),
                "status" to status.toString(), "recurrenceId" to recurrenceId)
        }
        jdbcTemplate.update(sql, paramMap)

        deleteItemsByMoneyOperId(domainAggregate.id)
        saveItems(domainAggregate.items)

        tagDao.updateLinks(domainAggregate.tags, domainAggregate.id, "operation")
    }

    private fun saveItems(moneyOperItems: List<MoneyOperItem>) {
        moneyOperItems.forEach { saveItem(it) }
    }

    private fun saveItem(moneyOperItem: MoneyOperItem) {
        val sql = """
            insert into money_oper_item(id, oper_id, balance_id, value, performed, index, bs_id, repayment_schedule)
                values(:id, :operId, :balanceId, :value, :performed, :index, :bsId, :repaymentSchedule)
            on conflict(id) do update set  
                oper_id = :operId, balance_id = :balanceId, value = :value, performed = :performed, index = :index,
                bs_id = :bsId, repayment_schedule = :repaymentSchedule
        """.trimIndent()
        val paramMap = with(moneyOperItem) {
            mapOf(
                "id" to id,
                "operId" to moneyOperId,
                "balanceId" to balanceId,
                "value" to value,
                "performed" to performed,
                "index" to index,
                "bsId" to  ApiRequestContextHolder.balanceSheet.id,
                "repaymentSchedule" to repaymentSchedule?.let { gson.toJsonb(it) }
            )
        }
        jdbcTemplate.update(sql, paramMap)
    }

    override fun findById(id: UUID): MoneyOper = findByIdOrNull(id)!!

    override fun findByIdOrNull(id: UUID): MoneyOper? {
        val sql = "select * from money_oper where id = :id"
        return jdbcTemplate.query(sql, mapOf("id" to id), rowMapper).firstOrNull()
    }

    override fun exists(id: UUID): Boolean = findByIdOrNull(id) != null

    override fun findByBalanceSheetAndStatus(balanceSheet: BalanceSheet, status: MoneyOperStatus, pageable: Pageable): Page<MoneyOper> {
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

    override fun findByBalanceSheetAndStatusAndPerformed(balanceSheet: BalanceSheet,
        status: MoneyOperStatus, performed: LocalDate, pageable: Pageable): Page<MoneyOper> {
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

    override fun findByBalanceSheetAndStatusAndPerformed(bsId: UUID, status: MoneyOperStatus, performed: LocalDate): List<MoneyOper> {
        val sql = """
            select * from money_oper 
            where balance_sheet_id = :bsId and status = :status and trn_date = :performed 
            order by date_num 
        """.trimIndent()
        val paramMap =
            mapOf("bsId" to bsId, "status" to status.toString(), "performed" to performed)
        return jdbcTemplate.query(sql, paramMap, rowMapper)
    }

    override fun findByBalanceSheetAndStatusAndPerformedGreaterThan(status: MoneyOperStatus,
        performed: LocalDate): List<MoneyOper> {
        val sql = """
            select * from money_oper 
            where balance_sheet_id = :bsId and status = :status and trn_date > :performed 
            order by date_num 
        """.trimIndent()
        val paramMap = mapOf(
            "bsId" to ApiRequestContextHolder.balanceSheet.id,
            "status" to status.toString(),
            "performed" to performed)
        return jdbcTemplate.query(sql, paramMap, rowMapper)
    }

    override fun findByBalanceSheetAndValueOrderByPerformedDesc(balanceSheet: BalanceSheet, absValue: BigDecimal,
        pageable: Pageable): Page<MoneyOper> {
        val total = run {
            val sql = """
                select count(*)
                from (
                    select distinct oper_id
                    from money_oper_item
                    where bs_id = :bsId and abs(value) = :value
                    ) t
            """.trimIndent()
            val paramMap =
                mapOf("bsId" to balanceSheet.id, "value" to absValue)
            jdbcTemplate.queryForObject(sql, paramMap, Long::class.java)!!
        }
        val sql = """
            select distinct mo.* 
            from money_oper_item moi
            join money_oper mo on mo.id = moi.oper_id
            where moi.bs_id = :bsId and abs(moi.value) = :value
            order by mo.trn_date desc, mo.date_num desc
            limit :limit
            offset :offset    
        """.trimIndent()
        val paramMap = mapOf("bsId" to balanceSheet.id, "value" to absValue,
            "limit" to pageable.pageSize, "offset" to pageable.offset)
        val list = jdbcTemplate.query(sql, paramMap, rowMapper)
        return PageImpl(list, pageable, total)
    }

    override fun findByPerformedBetweenAndMoneyOperStatus(startDate: LocalDate, finishDate: LocalDate,
        status: MoneyOperStatus): List<MoneyOper> {
        val sql = """
            select o.* 
            from money_oper o  
            where o.balance_sheet_id = :bsId 
                and o.trn_date between :startDate and :finishDate
                and o.status = :status
        """.trimIndent()
        val bsId = ApiRequestContextHolder.balanceSheet.id
        val paramMap = mapOf("bsId" to bsId,
            "startDate" to startDate, "finishDate" to finishDate, "status" to status.toString())
        return jdbcTemplate.query(sql, paramMap, rowMapper)
    }

    override fun findByCreditCardAndDateBetweenAndMoneyOperStatus(startDate: LocalDate, finishDate: LocalDate,
            status: MoneyOperStatus): List<MoneyOper> {
        val sql = """
            select o.* 
            from money_oper o
            join money_oper_item i on i.oper_id = o.id
            where o.balance_sheet_id = :bsId 
                and o.status = :status
                and i.repayment_schedule is not null    
                and (i.performed between :startDate and :finishDate 
                    or (i.repayment_schedule -> 0 ->> 'endDate')::date between :startDate and :finishDate)
                and (i.repayment_schedule -> 0 ->> 'endDate')::date > i.performed     
        """.trimIndent()
        val bsId = ApiRequestContextHolder.balanceSheet.id
        val paramMap = mapOf("bsId" to bsId,
            "startDate" to startDate, "finishDate" to finishDate, "status" to status.toString())
        return jdbcTemplate.query(sql, paramMap, rowMapper)
    }

    override fun getCurrentCreditCardDebt(currentDate: LocalDate): BigDecimal {
        val sql = """
            select sum(i.value) 
            from money_oper o
                join money_oper_item i on i.oper_id = o.id
            where o.balance_sheet_id = :bsId 
                and o.status = 'done'
                and i.repayment_schedule is not null    
                and (i.repayment_schedule -> 0 ->> 'endDate')::date > :currentDate
        """.trimIndent()
        val bsId = ApiRequestContextHolder.balanceSheet.id
        val paramMap = mapOf("bsId" to bsId, "currentDate" to currentDate)
        return jdbcTemplate.queryForObject(sql, paramMap, BigDecimal::class.java) ?: BigDecimal("0.00")
    }

    private fun findItemsByMoneyOperId(moneyOperId: UUID): List<MoneyOperItem> {
        val sql = "select * from money_oper_item where oper_id = :operId order by index"
        return jdbcTemplate.query(sql, mapOf("operId" to moneyOperId), itemRowMapper)
    }

    override fun existsByBalance(balance: Balance): Boolean {
        val sql = "select count(*) from money_oper_item where balance_id = :balanceId"
        val count = jdbcTemplate.queryForObject(sql, mapOf("balanceId" to balance.id), Int::class.java)!!
        return count > 0
    }

    private fun deleteItemsByMoneyOperId(moneyOperId: UUID) {
        val sql = "delete from money_oper_item where oper_id = :operId"
        jdbcTemplate.update(sql, mapOf("operId" to moneyOperId))
    }

    private val rowMapper: (rs: ResultSet, rowNum: Int) -> MoneyOper = { rs, _ ->
        val id = UUID.fromString(rs.getString("id"))
        val items = findItemsByMoneyOperId(id).toMutableList()
        val status = rs.getString("status").let { MoneyOperStatus.valueOf(it) }
        val performed = rs.getDate("trn_date").toLocalDate()
        val dateNum = rs.getInt("date_num")
        val tags = tagDao.findByObjId(id)
        val comment = rs.getString("comment")
        val period = rs.getString("period")?.let { Period.valueOf(it) }
        val recurrenceId = rs.getString("recurrence_id")?.let { UUID.fromString(it) }
        MoneyOper(id, items, status, performed, tags, comment, period, recurrenceId, dateNum).apply {
            this.created = rs.getTimestamp("created_ts")
        }
    }

    private val itemRowMapper: (rs: ResultSet, rowNum: Int) -> MoneyOperItem = { rs, _ ->
        val id = UUID.fromString(rs.getString("id"))
        val moneyOperId = UUID.fromString(rs.getString("oper_id"))
        val balance = UUID.fromString(rs.getString("balance_id")).let { balanceDao.findById(it) }
        val value = rs.getBigDecimal("value")
        val performed = rs.getDate("performed").toLocalDate()
        val index = rs.getInt("index")
        MoneyOperItem.of(moneyOperId, balance, value, performed, index, id)
    }
}