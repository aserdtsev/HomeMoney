package ru.serdtsev.homemoney.port.dao

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class AccountDao(private val jdbcTemplate: NamedParameterJdbcTemplate) {
    fun findNameById(id: UUID): String = findNameByIdOrNull(id)!!

    fun findNameByIdOrNull(id: UUID): String? {
        val sql = "select name from account where id = :id"
        return jdbcTemplate.queryForList(sql, mapOf("id" to id), String::class.java).firstOrNull()
    }
}