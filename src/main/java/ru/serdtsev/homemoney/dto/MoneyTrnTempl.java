package ru.serdtsev.homemoney.dto;

import javax.xml.bind.annotation.XmlTransient;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Шаблон операции
 */
public class MoneyTrnTempl {
  public static Date calcNextDate(Date origDate, MoneyTrn.Period period) {
    LocalDate origLocalDate = origDate.toLocalDate();
    LocalDate nextDate = null;
    switch (period) {
      case month: nextDate = origLocalDate.plusMonths(1); break;
      case quarter: nextDate = origLocalDate.plusMonths(3); break;
      case year: nextDate = origLocalDate.plusYears(1); break;
    }
    return Date.valueOf(nextDate);
  }

  public enum Status { active, deleted }
  private UUID id;
  private Status status;
  private UUID sampleId;
  private UUID lastMoneyTrnId;
  private java.sql.Date nextDate;
  private MoneyTrn.Period period;
  private UUID fromAccId;
  private String fromAccName;
  private UUID toAccId;
  private String toAccName;
  private String type;
  private BigDecimal amount;
  private String comment;
  private List<String> labels;

  @SuppressWarnings("unused")
  public MoneyTrnTempl() {
  }

  public MoneyTrnTempl(UUID id, UUID sampleId, UUID lastMoneyTrnId, Date nextDate, MoneyTrn.Period period,
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

  public MoneyTrn.Period getPeriod() {
    return period;
  }

  public void setPeriod(MoneyTrn.Period period) {
    this.period = period;
  }

  public UUID getFromAccId() {
    return fromAccId;
  }

  public void setFromAccId(UUID fromAccId) {
    this.fromAccId = fromAccId;
  }

  public String getFromAccName() {
    return fromAccName;
  }

  public void setFromAccName(String fromAccName) {
    this.fromAccName = fromAccName;
  }

  public UUID getToAccId() {
    return toAccId;
  }

  public void setToAccId(UUID toAccId) {
    this.toAccId = toAccId;
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

  @XmlTransient
  public String getLabelsAsString() {
    return String.join(",", getLabels());
  }

  public void setLabels(List<String> labels) {
    this.labels = labels;
  }
}
