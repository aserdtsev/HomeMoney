package ru.serdtsev.homemoney.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BsStat {
  private UUID bsId;
  private Date fromDate;
  private Date toDate;
  private BigDecimal incomeAmount = BigDecimal.ZERO;
  private BigDecimal chargesAmount = BigDecimal.ZERO;
  private List<BsDayStat> dayStats;
  private List<CategoryStat> categories;

  @JsonIgnore
  private HashMap<Account.Type, BigDecimal> saldoMap = new HashMap<>();

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BsStat() {
  }

  public BsStat(UUID bsId, Date fromDate, Date toDate) {
    this.bsId = bsId;
    this.fromDate = fromDate;
    this.toDate = toDate;
  }

  public UUID getBsId() {
    return bsId;
  }

  public void setBsId(UUID bsId) {
    this.bsId = bsId;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public Date getFromDate() {
    return fromDate;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public Date getToDate() {
    return toDate;
  }

  public BigDecimal getIncomeAmount() {
    return incomeAmount;
  }

  public void setIncomeAmount(BigDecimal incomeAmount) {
    this.incomeAmount = incomeAmount;
  }

  public BigDecimal getChargesAmount() {
    return chargesAmount;
  }

  public void setChargesAmount(BigDecimal chargesAmount) {
    this.chargesAmount = chargesAmount;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public List<BsDayStat> getDayStats() {
    return dayStats;
  }

  public void setDayStats(List<BsDayStat> dayStats) {
    this.dayStats = dayStats;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public List<CategoryStat> getCategories() {
    return categories;
  }

  public void setCategories(List<CategoryStat> categories) {
    this.categories = categories;
  }

  public HashMap<Account.Type, BigDecimal> getSaldoMap() {
    return saldoMap;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BigDecimal getFreeAmount() {
    return getDebitSaldo().subtract(getReserveSaldo());
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BigDecimal getReserveSaldo() {
    return saldoMap.getOrDefault(Account.Type.reserve, BigDecimal.ZERO);
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BigDecimal getTotalSaldo() {
    return getDebitSaldo().add(getCreditSaldo()).add(getAssetSaldo());
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BigDecimal getDebitSaldo() {
    return getSaldo(Account.Type.debit);
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BigDecimal getCreditSaldo() {
    return getSaldo(Account.Type.credit);
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BigDecimal getAssetSaldo() {
    return getSaldo(Account.Type.asset);
  }

  private BigDecimal getSaldo(Account.Type type) {
    return saldoMap.getOrDefault(type, BigDecimal.ZERO);
  }
}
