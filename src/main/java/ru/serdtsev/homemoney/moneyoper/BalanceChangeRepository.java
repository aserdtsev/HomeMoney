package ru.serdtsev.homemoney.moneyoper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.math.BigDecimal;
import java.util.UUID;

public interface BalanceChangeRepository extends PagingAndSortingRepository<BalanceChange, UUID> {
  Page<BalanceChange> findByValueOrValue(BigDecimal positiveValue, BigDecimal negativeValue, Pageable pageable);
}
