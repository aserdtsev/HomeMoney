package ru.serdtsev.homemoney.port.dao

import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.serdtsev.homemoney.domain.model.User
import ru.serdtsev.homemoney.domain.repository.UserRepository
import java.sql.ResultSet
import java.util.*

@Repository
class UserDao(private val jdbcTemplate: NamedParameterJdbcTemplate) : DomainModelDao<User>, UserRepository {
    override fun save(domainAggregate: User) {
        val sql = """
            insert into app_user(id, balance_sheet_id, email, pwd_hash)
            values(:id, :bsId, :email, :pwdHash)
            on conflict(id) do update set
                id = :id, balance_sheet_id = :bsId, email = :email, pwd_hash = :pwdHash
            
        """.trimIndent()
        val paramMap = with(domainAggregate) {
            mapOf("id" to id, "bsId" to bsId, "email" to email, "pwdHash" to pwdHash)
        }
        jdbcTemplate.update(sql, paramMap)
    }

    @Cacheable("UserByEmail", unless = "#result == null")
    override fun findByEmail(email: String): User? {
        val sql = """
            select id, balance_sheet_id, email, pwd_hash 
            from app_user 
            where email = :email
        """.trimIndent()
        return jdbcTemplate.query(sql, mapOf("email" to email), rowMapper).firstOrNull()
    }

    private val rowMapper: (rs: ResultSet, rowNum: Int) -> User = { rs, _ ->
        val id = UUID.fromString(rs.getString("id"))
        val bsId = UUID.fromString(rs.getString("balance_sheet_id"))
        val email = rs.getString("email")
        val pwdHash = rs.getString("pwd_hash")
        User(id, bsId, email, pwdHash)
    }
}