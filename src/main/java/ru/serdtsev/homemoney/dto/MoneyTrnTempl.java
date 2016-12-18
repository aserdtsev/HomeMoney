package ru.serdtsev.homemoney.dto;

import ru.serdtsev.homemoney.dto.MoneyTrn.Period;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MoneyTrnTempl {
  private UUID id;
  private Status status;
  private UUID sampleId;
  private UUID lastMoneyTrnId;
  private Date nextDate;
  private Period period;
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

  public void setSampleId(UUID sampleId) {
    this.sampleId = sampleId;
  }

  public UUID getLastMoneyTrnId() {
    return lastMoneyTrnId;
  }

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

  public UUID getFromAccId() {
    return fromAccId;
  }

  public void setFromAccId(UUID fromAccId) {
    this.fromAccId = fromAccId;
  }

  public UUID getToAccId() {
    return toAccId;
  }

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

  public List getLabels() {
    return labels;
  }

  public void setLabels(List labels) {
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

  public void setToAmount(BigDecimal toAmount) {
    this.toAmount = toAmount;
  }

  public String getToCurrencyCode() {
    return toCurrencyCode;
  }

  public void setToCurrencyCode(String toCurrencyCode) {
    this.toCurrencyCode = toCurrencyCode;
  }

  public String getFromAccName() {
    return fromAccName;
  }

  public void setFromAccName(String fromAccName) {
    this.fromAccName = fromAccName;
  }

  public String getToAccName() {
    return toAccName;
  }

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

  public String getToCurrencySymbol() {
    return Currency.getInstance(toCurrencyCode).getSymbol();
  }

  public String getLabelsAsString() {
    return labels.stream().collect(Collectors.joining(","));
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
    deleted;
  }
}
