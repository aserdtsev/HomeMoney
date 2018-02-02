package ru.serdtsev.homemoney.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import ru.serdtsev.homemoney.account.AccountType;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Обороты за день по типу счета
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"operDate", "accountType"})
public class Turnover {
  @Nonnull private final LocalDate operDate;
  @Nonnull private final AccountType accountType;
  /** Сумма оборотов со знаком */
  @Nonnull private BigDecimal amount = BigDecimal.ZERO;

  public void plus(BigDecimal value) {
    amount = amount.add(value);
  }


}
