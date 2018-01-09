package ru.serdtsev.homemoney.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.serdtsev.homemoney.account.AccountType;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Обороты за день по типу счета
 */
@Data
@AllArgsConstructor
public class Turnover {
  @Nonnull private LocalDate operDate;
  @Nonnull private AccountType accountType;
  /** Сумма оборотов со знаком */
  @Nonnull private BigDecimal amount = BigDecimal.ZERO;

  public Turnover(LocalDate operDate, AccountType accountType) {
    this.operDate = operDate;
    this.accountType = accountType;
  }

  public void plus(BigDecimal value) {
    amount = amount.add(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Turnover)) return false;
    if (!super.equals(o)) return false;
    Turnover turnover = (Turnover) o;
    return Objects.equals(operDate, turnover.operDate) &&
        accountType == turnover.accountType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), operDate, accountType);
  }
}
