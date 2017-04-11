package ru.serdtsev.homemoney.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.serdtsev.homemoney.account.AccountType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;

public class BsDayStat {
  private Long date;
  private BigDecimal incomeAmount = BigDecimal.ZERO;
  private BigDecimal chargeAmount = BigDecimal.ZERO;
  private HashMap<AccountType, BigDecimal> saldoMap = new HashMap<>();
  private HashMap<AccountType, BigDecimal> deltaMap = new HashMap<>();

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BsDayStat() {
  }

  public BsDayStat(Long date) {
    this.date = date;
  }

  public Long getDate() {
    return date;
  }

  public void setDate(Long date) {
    this.date = date;
  }

  public BigDecimal getIncomeAmount() {
    return incomeAmount;
  }

  public void setIncomeAmount(BigDecimal incomeAmount) {
    this.incomeAmount = incomeAmount;
  }

  public BigDecimal getChargeAmount() {
    return chargeAmount;
  }

  public void setChargeAmount(BigDecimal chargeAmount) {
    this.chargeAmount = chargeAmount;
  }

  @JsonIgnore
  public LocalDate getDateAsLocalDate() {
    return (new java.sql.Date(date)).toLocalDate();
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BigDecimal getTotalSaldo() {
    return getSaldo(AccountType.debit).add(getSaldo(AccountType.credit)).add(getSaldo(AccountType.asset));
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BigDecimal getFreeAmount() {
    return getSaldo(AccountType.debit).subtract(getReserveSaldo());
  }

  private BigDecimal getSaldo(AccountType type) {
    return saldoMap.getOrDefault(type, BigDecimal.ZERO);
  }

  public void setSaldo(AccountType type, BigDecimal value) {
    saldoMap.put(type, value.plus());
  }

  public BigDecimal getDelta(AccountType type) {
    return deltaMap.getOrDefault(type, BigDecimal.ZERO);
  }

  public void setDelta(AccountType type, BigDecimal amount) {
    deltaMap.put(type, amount);
  }

  private BigDecimal getReserveSaldo() {
    return getSaldo(AccountType.reserve);
  }

}
