package ru.serdtsev.homemoney.dto;


import ru.serdtsev.homemoney.account.BalanceDto;

import java.math.BigDecimal;

public class Reserve extends BalanceDto {
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
