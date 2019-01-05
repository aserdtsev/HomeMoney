package ru.serdtsev.homemoney.balancesheet

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

import java.util.UUID

interface BalanceSheetRepository : CrudRepository<BalanceSheet, UUID>
