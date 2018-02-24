package ru.serdtsev.homemoney.account;

import org.springframework.data.repository.CrudRepository;
import ru.serdtsev.homemoney.account.model.Reserve;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import java.util.UUID;

public interface ReserveRepository  extends CrudRepository<Reserve, UUID> {
  Iterable<Reserve> findByBalanceSheet(BalanceSheet balanceSheet);
}
