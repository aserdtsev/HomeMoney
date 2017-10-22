package ru.serdtsev.homemoney.moneyoper;

import org.springframework.data.repository.CrudRepository;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import java.util.UUID;
import java.util.stream.Stream;

public interface RecurrenceOperRepo extends CrudRepository<RecurrenceOper, UUID> {
  Stream<RecurrenceOper> findByBalanceSheet(BalanceSheet balanceSheet);
}
