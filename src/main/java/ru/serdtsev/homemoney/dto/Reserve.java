package ru.serdtsev.homemoney.dto;

import java.math.BigDecimal;
import java.util.Optional;

public class Reserve extends Balance {
  private Optional<BigDecimal> target;

  @SuppressWarnings("unused")
  public Reserve() {
    super();
  }

  public BigDecimal getTarget() {
    return target.orElse(BigDecimal.ZERO);
  }

  public void setTarget(BigDecimal target) {
    this.target = Optional.ofNullable(target);
  }
}
