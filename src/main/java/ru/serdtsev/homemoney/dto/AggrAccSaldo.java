package ru.serdtsev.homemoney.dto;

import java.math.BigDecimal;

public class AggrAccSaldo {
  private Account.Type type;
  private BigDecimal saldo;

  public AggrAccSaldo() {
  }

  public Account.Type getType() {
    return type;
  }

  public void setType(Account.Type type) {
    this.type = type;
  }

  public BigDecimal getSaldo() {
    return saldo;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setSaldo(BigDecimal saldo) {
    this.saldo = saldo;
  }
}
