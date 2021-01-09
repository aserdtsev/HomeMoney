package ru.serdtsev.homemoney.user

import java.util.*
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "auth_token")
class UserAuthToken(@Id val token: UUID, userId: UUID) {
    var userId: UUID = userId
        private set
}