package ru.serdtsev.homemoney.dto;


import java.math.BigDecimal;

public class Reserve extends Balance {
  private BigDecimal target;

  public Reserve() {
    this(null);
  }

  public Reserve(BigDecimal target) {
    this.target = target;
  }

  public BigDecimal getTarget() {
    return target != null ? target : BigDecimal.ZERO;
  }

  public void setTarget(BigDecimal target) {
    this.target = target;
  }
}
