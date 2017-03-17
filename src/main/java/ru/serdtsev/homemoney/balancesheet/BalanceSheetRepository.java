package ru.serdtsev.homemoney.balancesheet;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface BalanceSheetRepository extends CrudRepository<BalanceSheet, UUID> {
}
