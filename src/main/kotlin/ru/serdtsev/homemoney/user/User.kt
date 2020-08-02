package ru.serdtsev.homemoney.user

import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "users")
class User(
        @Id @Column(name = "user_id") val id: UUID,
        @Column(name = "bs_id") val bsId: UUID,
        @Column(length = 100) val email: String,
        @Column(name = "pwd_hash", length = 50) val pwdHash: String
)