package ru.serdtsev.homemoney.dto;

import ru.serdtsev.homemoney.HmException;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MoneyTrn {
  private UUID id;
  private MoneyTrn.Status status;
  private Date trnDate;
  private Integer dateNum;
  private UUID fromAccId;
  private UUID toAccId;
  private UUID parentId;
  private BigDecimal amount;
  private String currencyCode;
  private BigDecimal toAmount;
  private String toCurrencyCode;
  private String comment;
  private Timestamp createdTs;
  private MoneyTrn.Period period;
  private List<String> labels;
  private UUID templId;
  private String fromAccName;
  private String toAccName;
  private String type;

  public MoneyTrn() {}

  public MoneyTrn(UUID id, Status status, Date trnDate, UUID fromAccId, UUID toAccId, BigDecimal amount, Period period,
      String comment) {
    this(id, status, trnDate, fromAccId, toAccId, amount, period, comment,
        null, null, null, null, null);
  }

  public MoneyTrn(UUID id, Status status, Date trnDate, UUID fromAccId, UUID toAccId, BigDecimal amount, Period period,
      String comment, List<String> labels, Integer dateNum, UUID parentId, UUID templId, Timestamp createdTs) {
    if (amount.compareTo(BigDecimal.ZERO) == 0) {
      throw new HmException(HmException.Code.WrongAmount);
    }
    this.id = id;
    this.status = status;
    this.trnDate = trnDate;
    this.dateNum = dateNum;
    this.fromAccId = fromAccId;
    this.toAccId = toAccId;
    this.parentId = parentId;
    this.amount = amount;
    this.comment = comment;
    this.createdTs = createdTs != null ? createdTs : Timestamp.valueOf(LocalDateTime.now());
    this.period = period;
    this.labels = labels;
    this.templId = templId;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Date getTrnDate() {
    return trnDate;
  }

  public void setTrnDate(Date trnDate) {
    this.trnDate = trnDate;
  }

  public Integer getDateNum() {
    return dateNum;
  }

  public void setDateNum(Integer dateNum) {
    this.dateNum = dateNum;
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

  public UUID getParentId() {
    return parentId;
  }

  public void setParentId(UUID parentId) {
    this.parentId = parentId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
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

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Timestamp getCreatedTs() {
    return createdTs;
  }

  public void setCreatedTs(Timestamp createdTs) {
    this.createdTs = createdTs;
  }

  public Period getPeriod() {
    return period != null ? period : Period.month;
  }

  public void setPeriod(Period period) {
    this.period = period;
  }

  public List<String> getLabels() {
    return labels != null ? labels : new ArrayList<>(0);
  }

  public void setLabels(List<String> labels) {
    this.labels = labels;
  }

  public UUID getTemplId() {
    return templId;
  }

  public void setTemplId(UUID templId) {
    this.templId = templId;
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

  public Boolean isMonoCurrencies() {
    return currencyCode.equals(toCurrencyCode);
  }

  public Boolean crucialEquals(MoneyTrn other) {
    if (!equals(other)) {
      throw new HmException(HmException.Code.IdentifiersDoNotMatch);
    }
    return trnDate.equals(other.trnDate)
        && fromAccId.equals(other.fromAccId) && toAccId.equals(other.toAccId)
        && amount.compareTo(other.amount) == 0
        && toAmount.compareTo(other.toAmount) == 0
        && status == other.status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MoneyTrn moneyTrn = (MoneyTrn) o;

    return getId() != null ? getId().equals(moneyTrn.getId()) : moneyTrn.getId() == null;
  }

  @Override
  public int hashCode() {
    return getId() != null ? getId().hashCode() : 0;
  }

  public enum Status {
    pending,
    done,
    cancelled;
  }

  public enum Period {
    month,
    quarter,
    year,
    single;
  }
}
