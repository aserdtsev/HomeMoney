package ru.serdtsev.homemoney.moneyoper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MoneyOperItemRepo extends PagingAndSortingRepository<MoneyOperItem, UUID> {
  Page<MoneyOperItem> findByBalanceSheetAndValueOrderByPerformedDesc(BalanceSheet balanceSheet, BigDecimal absValue, Pageable pageable);
  List<MoneyOperItem> findByBalanceSheetAndPerformedBetween(BalanceSheet balanceSheet, LocalDate startDate, LocalDate finishDate);
}
