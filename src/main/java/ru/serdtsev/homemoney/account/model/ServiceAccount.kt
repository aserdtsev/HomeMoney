package ru.serdtsev.homemoney.account.model

import ru.serdtsev.homemoney.balancesheet.BalanceSheet
import java.sql.Date
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@DiscriminatorValue("service")
@Table(name = "svc_accounts")
open class ServiceAccount(
        balanceSheet: BalanceSheet,
        name: String,
        created: Date,
        isArc: Boolean?
) : Account(balanceSheet, AccountType.service, name, created, isArc)