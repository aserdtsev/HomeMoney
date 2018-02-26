package ru.serdtsev.homemoney.moneyoper.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public class RecurrenceOperDto {
  private UUID id;
  private Status status;
  private UUID sampleId;
  private UUID lastMoneyTrnId;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate nextDate;
  private Period period;
  private List<MoneyOperItem> items;
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
  public RecurrenceOperDto() {
  }

  public RecurrenceOperDto(UUID id, UUID sampleId, UUID lastMoneyTrnId, LocalDate nextDate, Period period,
      UUID fromAccId, UUID toAccId, BigDecimal amount, String comment, List<String> labels,
      String currencyCode, String toCurrencyCode, String fromAccName, String toAccName, String type) {
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
    this.currencyCode = currencyCode;
    this.toCurrencyCode = toCurrencyCode;
    this.fromAccName = fromAccName;
    this.toAccName = toAccName;
    this.type = type;
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

  public LocalDate getNextDate() {
    return nextDate;
  }

  public void setNextDate(LocalDate nextDate) {
    this.nextDate = nextDate;
  }

  public Period getPeriod() {
    return period;
  }

  public void setPeriod(Period period) {
    this.period = period;
  }

  public List<MoneyOperItem> getItems() {
    return items;
  }

  public void setItems(List<MoneyOperItem> items) {
    this.items = items;
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

  public enum Status {
    active,
    deleted
  }
}
