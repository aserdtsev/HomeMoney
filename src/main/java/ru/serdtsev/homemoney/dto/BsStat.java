package ru.serdtsev.homemoney.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.serdtsev.homemoney.account.AccountType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BsStat {
  private UUID bsId;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate fromDate;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate toDate;
  private BigDecimal incomeAmount = BigDecimal.ZERO;
  private BigDecimal chargesAmount = BigDecimal.ZERO;
  private List<BsDayStat> dayStats;
  private List<CategoryStat> categories;

  @JsonIgnore
  private HashMap<AccountType, BigDecimal> saldoMap = new HashMap<>();

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BsStat() {
  }

  public BsStat(UUID bsId, LocalDate fromDate, LocalDate toDate) {
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
  public LocalDate getFromDate() {
    return fromDate;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public LocalDate getToDate() {
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

  public HashMap<AccountType, BigDecimal> getSaldoMap() {
    return saldoMap;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BigDecimal getFreeAmount() {
    return getDebitSaldo().subtract(getReserveSaldo());
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BigDecimal getReserveSaldo() {
    return saldoMap.getOrDefault(AccountType.reserve, BigDecimal.ZERO);
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BigDecimal getTotalSaldo() {
    return getDebitSaldo().add(getCreditSaldo()).add(getAssetSaldo());
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BigDecimal getDebitSaldo() {
    return getSaldo(AccountType.debit);
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BigDecimal getCreditSaldo() {
    return getSaldo(AccountType.credit);
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BigDecimal getAssetSaldo() {
    return getSaldo(AccountType.asset);
  }

  private BigDecimal getSaldo(AccountType type) {
    return saldoMap.getOrDefault(type, BigDecimal.ZERO);
  }
}
