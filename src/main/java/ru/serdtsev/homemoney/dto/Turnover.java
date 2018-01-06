package ru.serdtsev.homemoney.dto;

import ru.serdtsev.homemoney.account.AccountType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Turnover {
  private LocalDate operDate;
  private AccountType fromAccType;
  private AccountType toAccType;
  private BigDecimal amount = BigDecimal.ZERO;

  @SuppressWarnings({"unused", "WeakerAccess"})
  public Turnover() {
  }

  public Turnover(LocalDate operDate, AccountType fromAccType, AccountType toAccType) {
    this.operDate = operDate;
    this.fromAccType = fromAccType;
    this.toAccType = toAccType;
  }

  public LocalDate getOperDate() {
    return operDate;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setOperDate(LocalDate trnDate) {
    this.operDate = trnDate;
  }

  public AccountType getFromAccType() {
    return fromAccType;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setFromAccType(AccountType fromAccType) {
    this.fromAccType = fromAccType;
  }

  public AccountType getToAccType() {
    return toAccType;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setToAccType(AccountType toAccType) {
    this.toAccType = toAccType;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Turnover turnover = (Turnover) o;

    return (getOperDate() != null ? getOperDate().equals(turnover.getOperDate()) : turnover.getOperDate() == null)
        && getFromAccType() == turnover.getFromAccType() && getToAccType() == turnover.getToAccType();
  }

  @Override
  public int hashCode() {
    int result = getOperDate() != null ? getOperDate().hashCode() : 0;
    result = 31 * result + (getFromAccType() != null ? getFromAccType().hashCode() : 0);
    result = 31 * result + (getToAccType() != null ? getToAccType().hashCode() : 0);
    return result;
  }
}
