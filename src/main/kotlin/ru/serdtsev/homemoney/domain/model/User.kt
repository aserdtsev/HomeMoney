package ru.serdtsev.homemoney.domain.model

import ru.serdtsev.homemoney.domain.event.DomainEvent
import java.util.*

class User(
        val id: UUID,
        val bsId: UUID,
        val email: String,
        val pwdHash: String
) : DomainEvent