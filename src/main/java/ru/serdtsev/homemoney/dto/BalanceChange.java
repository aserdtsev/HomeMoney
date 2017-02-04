package ru.serdtsev.homemoney.dto;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.UUID;

public class BalanceChange {
  private UUID id;
  private UUID operId;
  private UUID balanceId;
  private BigDecimal value;
  private Date made;
  private int index;

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

  public Date getMade() {
    return made;
  }

  public void setMade(Date made) {
    this.made = made;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }
}
