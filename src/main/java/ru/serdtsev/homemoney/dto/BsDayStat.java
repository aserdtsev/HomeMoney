package ru.serdtsev.homemoney.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;

public class BsDayStat {
  private Long date;
  private BigDecimal incomeAmount = BigDecimal.ZERO;
  private BigDecimal chargeAmount = BigDecimal.ZERO;
  private HashMap<Account.Type, BigDecimal> saldoMap = new HashMap<>();
  private HashMap<Account.Type, BigDecimal> deltaMap = new HashMap<>();

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
    return getSaldo(Account.Type.debit).add(getSaldo(Account.Type.credit)).add(getSaldo(Account.Type.asset));
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BigDecimal getFreeAmount() {
    return getSaldo(Account.Type.debit).subtract(getReserveSaldo());
  }

  private BigDecimal getSaldo(Account.Type type) {
    return saldoMap.getOrDefault(type, BigDecimal.ZERO);
  }

  public void setSaldo(Account.Type type, BigDecimal value) {
    saldoMap.put(type, value.plus());
  }

  public BigDecimal getDelta(Account.Type type) {
    return deltaMap.getOrDefault(type, BigDecimal.ZERO);
  }

  public void setDelta(Account.Type type, BigDecimal amount) {
    deltaMap.put(type, amount);
  }

  private BigDecimal getReserveSaldo() {
    return getSaldo(Account.Type.reserve);
  }

}
