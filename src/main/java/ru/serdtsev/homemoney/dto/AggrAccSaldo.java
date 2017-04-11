package ru.serdtsev.homemoney.dto;

import ru.serdtsev.homemoney.account.AccountType;

import java.math.BigDecimal;

public class AggrAccSaldo {
  private AccountType type;
  private BigDecimal saldo;

  public AggrAccSaldo() {
  }

  public AccountType getType() {
    return type;
  }

  public void setType(AccountType type) {
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
