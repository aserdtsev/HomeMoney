package ru.serdtsev.homemoney.moneyoper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus;

import java.sql.Date;
import java.util.UUID;
import java.util.stream.Stream;

public interface MoneyOperRepo extends PagingAndSortingRepository<MoneyOper, UUID> {
  Stream<MoneyOper> findByBalanceSheet(BalanceSheet balanceSheet);
  Page<MoneyOper> findByBalanceSheetAndStatus(BalanceSheet balanceSheet, MoneyOperStatus status, Pageable pageable);
  Page<MoneyOper> findByBalanceSheetAndStatusAndId(BalanceSheet balanceSheet, MoneyOperStatus status, UUID id, Pageable pageable);
  Page<MoneyOper> findByBalanceSheetAndStatusAndPerformed(BalanceSheet balanceSheet, MoneyOperStatus status, Date performed, Pageable pageable);
  Stream<MoneyOper> findByBalanceSheetAndStatusAndPerformed(BalanceSheet balanceSheet, MoneyOperStatus status, Date performed);
}
