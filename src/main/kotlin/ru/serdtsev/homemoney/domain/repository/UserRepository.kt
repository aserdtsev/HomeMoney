package ru.serdtsev.homemoney.domain.repository

import org.springframework.cache.annotation.Cacheable
import ru.serdtsev.homemoney.domain.model.User

interface UserRepository {
    @Cacheable("UserDao.findByEmail", condition = "#result != null")
    fun findByEmail(email: String): User?
}