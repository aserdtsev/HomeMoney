package ru.serdtsev.homemoney.dto;

import com.google.common.base.Objects;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.UUID;

public class BalanceChange {
  private UUID id;
  private UUID operId;
  private UUID balanceId;
  private BigDecimal value;
  private Date performed;
  private int index;

  @SuppressWarnings("unused")
  public BalanceChange() {
  }

  public BalanceChange(UUID id, UUID operId, UUID balanceId, BigDecimal value, Date performed, int index) {
    this.id = id;
    this.operId = operId;
    this.balanceId = balanceId;
    this.value = value;
    this.performed = performed;
    this.index = index;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getOperId() {
    return operId;
  }

  public void setOperId(UUID operId) {
    this.operId = operId;
  }

  public UUID getBalanceId() {
    return balanceId;
  }

  public void setBalanceId(UUID balanceId) {
    this.balanceId = balanceId;
  }

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  public Date getPerformed() {
    return performed;
  }

  public void setPerformed(Date performed) {
    this.performed = performed;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BalanceChange that = (BalanceChange) o;
    return Objects.equal(getId(), that.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getId());
  }
}
