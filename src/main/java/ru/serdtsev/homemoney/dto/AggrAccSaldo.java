package ru.serdtsev.homemoney.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Optional;

public class AggrAccSaldo implements Serializable {
  public Account.Type type;
  public Optional<BigDecimal> saldo;

  public Account.Type getType() {
    return type;
  }

  public void setType(Account.Type type) {
    this.type = type;
  }

  public BigDecimal getSaldo() {
    return saldo.orElse(BigDecimal.ZERO);
  }

  public void setSaldo(BigDecimal saldo) {
    this.saldo = Optional.of(saldo);
  }
}
