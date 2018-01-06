package ru.serdtsev.homemoney.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.serdtsev.homemoney.account.AccountType;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AggrAccSaldo {
  private AccountType type;
  private BigDecimal saldo;
}
