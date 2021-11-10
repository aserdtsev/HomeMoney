package ru.serdtsev.homemoney.user

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepo : CrudRepository<User, UUID> {
    fun findByEmail(email: String): User?
}