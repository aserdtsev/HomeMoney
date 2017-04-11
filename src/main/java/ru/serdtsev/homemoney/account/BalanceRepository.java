package ru.serdtsev.homemoney.account;

import org.springframework.data.repository.CrudRepository;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import java.util.UUID;

public interface BalanceRepository extends CrudRepository<Balance, UUID> {
  Iterable<Balance> findByBalanceSheet(BalanceSheet balanceSheet);
}
