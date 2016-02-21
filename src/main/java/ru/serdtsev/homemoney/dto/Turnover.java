package ru.serdtsev.homemoney.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;

public class Turnover implements Serializable {
  public Date trnDate;
  public Account.Type fromAccType;
  public Account.Type toAccType;
  public BigDecimal amount;

  public Turnover() {
  }

  public Turnover(Date trnDate, Account.Type fromAccType, Account.Type toAccType) {
    this.trnDate = trnDate;
    this.fromAccType = fromAccType;
    this.toAccType = toAccType;
    this.amount = BigDecimal.ZERO;
  }

  public Date getTrnDate() {
    return trnDate;
  }

  public void setTrnDate(Date trnDate) {
    this.trnDate = trnDate;
  }

  public Account.Type getFromAccType() {
    return fromAccType;
  }

  public void setFromAccType(Account.Type fromAccType) {
    this.fromAccType = fromAccType;
  }

  public Account.Type getToAccType() {
    return toAccType;
  }

  public void setToAccType(Account.Type toAccType) {
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

    if (!getTrnDate().equals(turnover.getTrnDate())) return false;
    if (getFromAccType() != turnover.getFromAccType()) return false;
    return getToAccType() == turnover.getToAccType();

  }

  @Override
  public int hashCode() {
    int result = getTrnDate().hashCode();
    result = 31 * result + getFromAccType().hashCode();
    result = 31 * result + getToAccType().hashCode();
    return result;
  }
}
