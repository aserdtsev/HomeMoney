package ru.serdtsev.homemoney.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.serdtsev.homemoney.account.AccountType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;

public class BsDayStat {
  @JsonIgnore
  private LocalDate localDate;
  private BigDecimal incomeAmount = BigDecimal.ZERO;
  private BigDecimal chargeAmount = BigDecimal.ZERO;
  private HashMap<AccountType, BigDecimal> saldoMap = new HashMap<>();
  private HashMap<AccountType, BigDecimal> deltaMap = new HashMap<>();

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BsDayStat() {
  }

  public BsDayStat(LocalDate localDate) {
    this.localDate = localDate;
  }

  public LocalDate getLocalDate() {
    return localDate;
  }

  public void setDate(LocalDate localDate) {
    this.localDate = localDate;
  }

  @JsonProperty("date")
  public Long getDate() {
    ZoneOffset zoneOffset = OffsetDateTime.now().getOffset();
    return localDate.atStartOfDay().toInstant(zoneOffset).toEpochMilli();
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
