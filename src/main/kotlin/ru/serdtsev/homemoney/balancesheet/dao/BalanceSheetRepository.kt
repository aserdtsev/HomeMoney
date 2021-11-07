package ru.serdtsev.homemoney.balancesheet.dao

import org.springframework.data.repository.CrudRepository
import ru.serdtsev.homemoney.balancesheet.model.BalanceSheet

import java.util.UUID

interface BalanceSheetRepository : CrudRepository<BalanceSheet, UUID>
