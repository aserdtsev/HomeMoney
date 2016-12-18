package ru.serdtsev.homemoney.dto;


import java.math.BigDecimal;

public class Reserve extends Balance {
  private BigDecimal target;

  public Reserve() {
  }

  public BigDecimal getTarget() {
    return target != null ? target : BigDecimal.ZERO;
  }

  public void setTarget(BigDecimal target) {
    this.target = target;
  }
}
