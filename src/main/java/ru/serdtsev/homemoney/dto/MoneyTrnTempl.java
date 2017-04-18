package ru.serdtsev.homemoney.dto;

import ru.serdtsev.homemoney.moneyoper.Period;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public class MoneyTrnTempl {
  private UUID id;
  private Status status;
  private UUID sampleId;
  private UUID lastMoneyTrnId;
  private Date nextDate;
  private Period period;
  private List<BalanceChange> balanceChanges;
  private UUID fromAccId;
  private UUID toAccId;
  private BigDecimal amount;
  private String comment;
  private List<String> labels;
  private String currencyCode;
  private BigDecimal toAmount;
  private String toCurrencyCode;
  private String fromAccName;
  private String toAccName;
  private String type;

  @SuppressWarnings({"unused", "WeakerAccess"})
  public MoneyTrnTempl() {
  }

  public MoneyTrnTempl(UUID id, UUID sampleId, UUID lastMoneyTrnId, Date nextDate, Period period,
      UUID fromAccId, UUID toAccId, BigDecimal amount, String comment, List<String> labels) {
    this.id = id;
    this.sampleId = sampleId;
    this.lastMoneyTrnId = lastMoneyTrnId;
    this.nextDate = nextDate;
    this.period = period;
    this.fromAccId = fromAccId;
    this.toAccId = toAccId;
    this.amount = amount;
    this.comment = comment;
    this.labels = labels;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Status getStatus() {
    return status != null ? status : Status.active;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public UUID getSampleId() {
    return sampleId;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setSampleId(UUID sampleId) {
    this.sampleId = sampleId;
  }

  public UUID getLastMoneyTrnId() {
    return lastMoneyTrnId;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setLastMoneyTrnId(UUID lastMoneyTrnId) {
    this.lastMoneyTrnId = lastMoneyTrnId;
  }

  public Date getNextDate() {
    return nextDate;
  }

  public void setNextDate(Date nextDate) {
    this.nextDate = nextDate;
  }

  public Period getPeriod() {
    return period;
  }

  public void setPeriod(Period period) {
    this.period = period;
  }

  public List<BalanceChange> getBalanceChanges() {
    return balanceChanges;
  }

  public void setBalanceChanges(List<BalanceChange> balanceChanges) {
    this.balanceChanges = balanceChanges;
  }

  public UUID getFromAccId() {
    return fromAccId;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setFromAccId(UUID fromAccId) {
    this.fromAccId = fromAccId;
  }

  public UUID getToAccId() {
    return toAccId;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setToAccId(UUID toAccId) {
    this.toAccId = toAccId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public List<String> getLabels() {
    return labels;
  }

  public void setLabels(List<String> labels) {
    this.labels = labels;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public BigDecimal getToAmount() {
    return toAmount;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setToAmount(BigDecimal toAmount) {
    this.toAmount = toAmount;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public String getToCurrencyCode() {
    return toCurrencyCode;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setToCurrencyCode(String toCurrencyCode) {
    this.toCurrencyCode = toCurrencyCode;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public String getFromAccName() {
    return fromAccName;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setFromAccName(String fromAccName) {
    this.fromAccName = fromAccName;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public String getToAccName() {
    return toAccName;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setToAccName(String toAccName) {
    this.toAccName = toAccName;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getCurrencySymbol() {
    return Currency.getInstance(currencyCode).getSymbol();
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public String getToCurrencySymbol() {
    return Currency.getInstance(toCurrencyCode).getSymbol();
  }

  public static java.sql.Date calcNextDate(java.sql.Date origDate, Period period) {
    LocalDate origLocalDate = origDate.toLocalDate();
    LocalDate nextDate;
    switch (period) {
      case month:
        nextDate = origLocalDate.plusMonths(1);
        break;
      case quarter:
        nextDate = origLocalDate.plusMonths(3);
        break;
      case year:
        nextDate =  origLocalDate.plusYears(1);
        break;
      default:
        nextDate = origDate.toLocalDate();
    }
    return Date.valueOf(nextDate);
  }

  public enum Status {
    active,
    deleted
  }
}
