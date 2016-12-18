package ru.serdtsev.homemoney.dto;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

public class Balance extends Account {
  private String currencyCode;
  private String currencySymbol;
  private BigDecimal value;
  private UUID reserveId;
  private BigDecimal creditLimit;
  private BigDecimal minValue;
  private Long num;

  @SuppressWarnings({"unused", "WeakerAccess"})
  public Balance() {
    super();
  }

  public Balance(UUID id, Type type, String name, String currencyCode, BigDecimal value) {
    super(id, type, name);
    this.currencyCode = currencyCode;
    this.value = value;
  }

  public Balance(UUID id, Type type, String name, String currencyCode, BigDecimal value, UUID reserveId,
      BigDecimal creditLimit, BigDecimal minValue, Long num) {
    super(id, type, name);
    this.currencyCode = currencyCode;
    this.value = value;
    this.reserveId = reserveId;
    this.minValue = minValue;
    this.creditLimit = creditLimit;
    this.num = num;
  }

  public String getCurrencyCode() {
    return currencyCode != null ?  currencyCode : "RUB";
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public String getCurrencySymbol() {
    return currencySymbol;
  }

  public void setCurrencySymbol(String currencySymbol) {
    this.currencySymbol = currencySymbol;
  }

  private Currency getCurrency() {
    assert currencyCode != null : this.toString();
    return Currency.getInstance(currencyCode);
  }

  public BigDecimal getValue() {
    return value != null ? value : BigDecimal.ZERO.setScale(getCurrency().getDefaultFractionDigits());
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  public UUID getReserveId() {
    return reserveId;
  }

  public void setReserveId(UUID reserveId) {
    this.reserveId = reserveId;
  }

  public BigDecimal getCreditLimit() {
    return creditLimit != null ? creditLimit : BigDecimal.ZERO.setScale(getCurrency().getDefaultFractionDigits());
  }

  public void setCreditLimit(BigDecimal creditLimit) {
    this.creditLimit = creditLimit;
  }

  public BigDecimal getMinValue() {
    return minValue != null ? minValue : BigDecimal.ZERO.setScale(getCurrency().getDefaultFractionDigits());
  }

  public void setMinValue(BigDecimal minValue) {
    this.minValue = minValue;
  }

  public Long getNum() {
    return num != null ? num : 0L;
  }

  public void setNum(Long num) {
    this.num = num;
  }

  @Override
  public String toString() {
    return "Balance{" +
        super.toString() + ", " +
        "currencyCode='" + currencyCode + '\'' +
        ", currencySymbol='" + currencySymbol + '\'' +
        ", value=" + value +
        ", reserveId=" + reserveId +
        ", creditLimit=" + creditLimit +
        ", minValue=" + minValue +
        ", num=" + num +
        "} ";
  }
}
