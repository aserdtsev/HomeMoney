package ru.serdtsev.homemoney.dto;

import javax.xml.bind.annotation.XmlTransient;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class BsDayStat {
  public java.sql.Date date;
  @XmlTransient
  private Map<Account.Type, BigDecimal> saldoMap = new HashMap<>();
  @XmlTransient
  private Map<Account.Type, BigDecimal> deltaMap = new HashMap<>();
  @XmlTransient
  public BigDecimal incomeAmount = BigDecimal.ZERO;
  @XmlTransient
  public BigDecimal chargeAmount = BigDecimal.ZERO;

  public BsDayStat(java.sql.Date date) {
    this.date = date;
  }

  public java.util.Date getDate() {
    return new java.util.Date(date.getTime());
  }

  public BigDecimal getTotalSaldo() {
    return getSaldo(Account.Type.debit)
        .add(getSaldo(Account.Type.credit))
        .add(getSaldo(Account.Type.asset));
  }

  public BigDecimal getFreeAmount() {
    return getSaldo(Account.Type.debit)
        .subtract(getReserveSaldo());
  }

  public BigDecimal getReserveSaldo() {
    return getSaldo(Account.Type.reserve);
  }

  public BigDecimal getIncomeAmount() {
    return incomeAmount;
  }

  public BigDecimal getChargeAmount() {
    return chargeAmount;
  }

  @XmlTransient
  public BigDecimal getSaldo(Account.Type type) {
    return saldoMap.getOrDefault(type, BigDecimal.ZERO);
  }

  @XmlTransient
  public BigDecimal setSaldo(Account.Type type, BigDecimal value) {
    return saldoMap.put(type, value.plus());
  }

  @XmlTransient
  public BigDecimal getDelta(Account.Type type) {
    return deltaMap.getOrDefault(type, BigDecimal.ZERO);
  }

  @XmlTransient
  public void setDelta(Account.Type type, BigDecimal amount) {
    deltaMap.put(type, amount);
  }

}
