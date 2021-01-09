package ru.serdtsev.homemoney.user

import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "app_user")
class User(
        @Id @Column val id: UUID,
        @Column(name = "balance_sheet_id") val bsId: UUID,
        @Column val email: String,
        @Column val pwdHash: String
)