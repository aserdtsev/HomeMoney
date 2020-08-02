package ru.serdtsev.homemoney.user

import org.springframework.data.repository.CrudRepository
import java.util.*

interface UserAuthTokenRepository : CrudRepository<UserAuthToken, UUID>