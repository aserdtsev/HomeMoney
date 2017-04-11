package ru.serdtsev.homemoney.dto;

import ru.serdtsev.homemoney.account.AccountType;

import java.math.BigDecimal;
import java.sql.Date;

public class Turnover {
  private Date trnDate;
  private AccountType fromAccType;
  private AccountType toAccType;
  private BigDecimal amount = BigDecimal.ZERO;

  @SuppressWarnings({"unused", "WeakerAccess"})
  public Turnover() {
  }

  public Turnover(Date trnDate, AccountType fromAccType, AccountType toAccType) {
    this.trnDate = trnDate;
    this.fromAccType = fromAccType;
    this.toAccType = toAccType;
  }

  public Date getTrnDate() {
    return trnDate;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setTrnDate(Date trnDate) {
    this.trnDate = trnDate;
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

    return (getTrnDate() != null ? getTrnDate().equals(turnover.getTrnDate()) : turnover.getTrnDate() == null)
        && getFromAccType() == turnover.getFromAccType() && getToAccType() == turnover.getToAccType();
  }

  @Override
  public int hashCode() {
    int result = getTrnDate() != null ? getTrnDate().hashCode() : 0;
    result = 31 * result + (getFromAccType() != null ? getFromAccType().hashCode() : 0);
    result = 31 * result + (getToAccType() != null ? getToAccType().hashCode() : 0);
    return result;
  }
}
