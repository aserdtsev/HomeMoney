package ru.serdtsev.homemoney.dto;


import ru.serdtsev.homemoney.account.BalanceDto;

import java.math.BigDecimal;

public class ReserveDto extends BalanceDto {
  private BigDecimal target;

  public ReserveDto() {
  }

  public BigDecimal getTarget() {
    return target != null ? target : BigDecimal.ZERO;
  }

  public void setTarget(BigDecimal target) {
    this.target = target;
  }
}
