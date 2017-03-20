package ru.serdtsev.homemoney.account;

import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Date;

@Entity
@Table(name= "reserves")
@DiscriminatorValue("reserve")
public class Reserve extends Balance {
  private BigDecimal target;

  protected Reserve() {
  }

  public Reserve(BalanceSheet balanceSheet, String name, Date created, Boolean isArc, String currencyCode,
      BigDecimal value, BigDecimal target) {
    super(balanceSheet, AccountType.reserve, name, created, isArc, currencyCode, value, null);
    this.target = target;
  }
}
