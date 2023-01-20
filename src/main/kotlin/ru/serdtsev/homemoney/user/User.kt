package ru.serdtsev.homemoney.user

import java.util.*

class User(
        val id: UUID,
        val bsId: UUID,
        val email: String,
        val pwdHash: String
)