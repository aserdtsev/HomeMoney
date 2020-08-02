package ru.serdtsev.homemoney.account.model

import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import java.sql.Date
import java.util.*
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@DiscriminatorValue("service")
@Table(name = "svc_accounts")
open class ServiceAccount(
        id: UUID,
        balanceSheet: BalanceSheet,
        name: String,
        createdDate: Date,
        isArc: Boolean?
) : Account(id, balanceSheet, AccountType.service, name, createdDate, isArc)