package ru.serdtsev.homemoney.dto;

import ru.serdtsev.homemoney.common.HmException;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public class MoneyTrn {
  private UUID id;
  private MoneyTrn.Status status;
  private Date trnDate;
  private Integer dateNum;
  private List<BalanceChange> balanceChanges;
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

  @SuppressWarnings({"unused", "WeakerAccess"})
  public MoneyTrn() {}

  public MoneyTrn(UUID id, Status status, Date trnDate, UUID fromAccId, UUID toAccId, BigDecimal amount, Period period,
      String comment) {
    this(id, status, trnDate, fromAccId, toAccId, amount, period, comment,
        null, null, null, null, null);
  }

  public MoneyTrn(UUID id, Status status, Date trnDate,
      UUID fromAccId, UUID toAccId, BigDecimal amount, Period period,
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

    this.balanceChanges = new ArrayList<>();
    balanceChanges.add(new BalanceChange(UUID.randomUUID(), id, fromAccId, amount.negate(), trnDate, 0));
    balanceChanges.add(new BalanceChange(UUID.randomUUID(), id, toAccId, toAmount, trnDate, 1));
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

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setTrnDate(Date trnDate) {
    this.trnDate = trnDate;
  }

  public Integer getDateNum() {
    return dateNum;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setDateNum(Integer dateNum) {
    this.dateNum = dateNum;
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

  public UUID getParentId() {
    return parentId;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
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

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Timestamp getCreatedTs() {
    return createdTs;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
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

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setTemplId(UUID templId) {
    this.templId = templId;
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

  public Boolean isMonoCurrencies() {
    return currencyCode.equals(toCurrencyCode);
  }

  public List<BalanceChange> getBalanceChanges() {
    return balanceChanges;
  }

  public void setBalanceChanges(List<BalanceChange> balanceChanges) {
    this.balanceChanges = balanceChanges;
  }

  /**
   * Возвращает true, поля операции, которые влияют на остатки в разрезе дат, одинаковые.
   */
  public Boolean essentialEquals(MoneyTrn other) {
    if (!equals(other)) {
      throw new HmException(HmException.Code.IdentifiersDoNotMatch);
    }
//    return trnDate.toLocalDate().equals(other.trnDate.toLocalDate())
//        && balanceChanges.stream().anyMatch(change -> other.balanceChanges.contains(change))
//        && other.balanceChanges.stream().anyMatch(change -> balanceChanges.contains(change))
//        && status == other.status;
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

  @Override
  public String toString() {
    return "MoneyTrn{" +
        "id=" + id +
        ", status=" + status +
        ", trnDate=" + trnDate +
        ", balanceChanges=" + balanceChanges +
        ", dateNum=" + dateNum +
        ", fromAccId=" + fromAccId +
        ", toAccId=" + toAccId +
        ", parentId=" + parentId +
        ", amount=" + amount +
        ", currencyCode='" + currencyCode + '\'' +
        ", toAmount=" + toAmount +
        ", toCurrencyCode='" + toCurrencyCode + '\'' +
        ", comment='" + comment + '\'' +
        ", createdTs=" + createdTs +
        ", period=" + period +
        ", labels=" + labels +
        ", templId=" + templId +
        ", fromAccName='" + fromAccName + '\'' +
        ", toAccName='" + toAccName + '\'' +
        ", type='" + type + '\'' +
        '}';
  }

  public enum Status {
    /**
     * в ожидании
     */
    pending,
    /**
     * ожидает повтора
     */
    recurrence,
    /**
     * выполнен
     */
    done,
    /**
     * выполнен (новый)
     */
    doneNew,
    /**
     * отменен
     */
    cancelled
  }

  public enum Period {
    month,
    quarter,
    year,
    single
  }
}
