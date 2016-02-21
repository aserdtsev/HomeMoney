package ru.serdtsev.homemoney.dto;

import ru.serdtsev.homemoney.HmException;

import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MoneyTrn implements Serializable {
  public enum Status { pending, done, cancelled }
  public enum Period { month, quarter, year, single }
  private UUID id;
  private Optional<Status> status;
  private Date trnDate;
  private Integer dateNum;
  private UUID fromAccId;
  private String fromAccName;
  private UUID toAccId;
  private String toAccName;
  private String type;
  private UUID parentId;
  private BigDecimal amount;
  private String comment;
  private Timestamp createdTs;
  private Optional<Period> period = Optional.empty();
  private List<String> labels = new ArrayList<>();
  private UUID templId;

  @SuppressWarnings("unused")
  public MoneyTrn() {
  }

  public MoneyTrn(UUID id, Status status, Date trnDate, Integer dateNum, UUID fromAccId, UUID toAccId, UUID parentId,
      BigDecimal amount, String comment, Timestamp createdTs, Period period, List<String> labels, UUID templId) {
    if (amount.compareTo(BigDecimal.ZERO) == 0) {
      throw new HmException(HmException.Code.AmountWrong);
    }
    setId(id);
    setStatus(status);
    setTrnDate(trnDate);
    setDateNum(dateNum);
    setFromAccId(fromAccId);
    setToAccId(toAccId);
    setParentId(parentId);
    setAmount(amount);
    setComment(comment);
    setCreatedTs(createdTs);
    setPeriod(period);
    setLabels(labels);
    setTemplId(templId);
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Status getStatus() {
    return status.get();
  }

  public void setStatus(Status status) {
    this.status = Optional.of(status);
  }

  public Timestamp getCreatedTs() {
    return createdTs;
  }

  public void setCreatedTs(Timestamp createdTs) {
    this.createdTs = createdTs;
  }

  public Date getTrnDate() {
    return trnDate;
  }

  public void setTrnDate(Date trnDate) {
    this.trnDate = Date.valueOf(trnDate.toLocalDate());
  }

  public Integer getDateNum() {
    return dateNum;
  }

  public void setDateNum(Integer dateNum) {
    this.dateNum = dateNum;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public UUID getFromAccId() {
    return fromAccId;
  }

  public void setFromAccId(UUID fromAccId) {
    this.fromAccId = fromAccId;
  }

  @SuppressWarnings("unused")
  public String getFromAccName() {
    return fromAccName;
  }

  @SuppressWarnings("unused")
  public void setFromAccName(String fromAccName) {
    this.fromAccName = fromAccName;
  }

  public UUID getToAccId() {
    return toAccId;
  }

  public void setToAccId(UUID toAccId) {
    this.toAccId = toAccId;
  }

  @SuppressWarnings("unused")
  public String getToAccName() {
    return toAccName;
  }

  @SuppressWarnings("unused")
  public void setToAccName(String toAccName) {
    this.toAccName = toAccName;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
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

  public Period getPeriod() {
    return period.orElse(Period.month);
  }

  public void setPeriod(Period period) {
    this.period = Optional.ofNullable(period);
  }

  public List<String> getLabels() {
    return labels != null ? labels : new ArrayList(0);
  }

  @XmlTransient
  public String getLabelsAsString() {
    return String.join(",", getLabels());
  }

  public void setLabels(List<String> labels) {
    this.labels = labels;
  }

  @SuppressWarnings("unused")
  public UUID getTemplId() {
    return templId;
  }

  @SuppressWarnings("unused")
  public void setTemplId(UUID templId) {
    this.templId = templId;
  }

  public boolean crucialEquals(MoneyTrn other) {
    if (!id.equals(other.id)) {
      throw new HmException(HmException.Code.IdentifiersDoNotMatch);
    }
    return getTrnDate().equals(other.getTrnDate())
        && fromAccId.equals(other.getFromAccId()) && toAccId.equals(other.getToAccId())
        && amount.compareTo(other.getAmount()) == 0 && getStatus().equals(other.getStatus());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MoneyTrn)) return false;
    MoneyTrn that = (MoneyTrn) o;
    return !(id != null ? !id.equals(that.id) : that.id != null);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}
