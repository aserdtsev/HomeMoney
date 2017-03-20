package ru.serdtsev.homemoney.account;

import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Date;

@Entity
@Table(name = "balances")
@DiscriminatorValue("balance")
public class Balance extends Account {
  @Column(name = "currency_code")
  private String currencyCode;
  
  private BigDecimal value;

  @OneToOne
  @JoinColumn(name = "reserve_id")
  private Reserve reserve;

  @Column(name = "credit_limit")
  private BigDecimal creditLimit;

  @Column(name = "min_value")
  private BigDecimal minValue;

  private Long num;

  @SuppressWarnings({"unused", "WeakerAccess"})
  public Balance() {
    super();
  }

  public Balance(BalanceSheet balanceSheet, AccountType type, String name, Date created, Boolean isArc, String currencyCode,
      BigDecimal value, BigDecimal minValue) {
    super(balanceSheet, type, name, created, isArc);
    this.currencyCode = currencyCode;
    this.value = value;
    this.minValue = minValue;
  }

}
