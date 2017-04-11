package ru.serdtsev.homemoney.account;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static ru.serdtsev.homemoney.utils.Utils.nvl;

public class BalanceDto extends AccountDto {
  private String currencyCode;
  private String currencySymbol;
  private BigDecimal value;
  private UUID reserveId;
  private BigDecimal creditLimit;
  private BigDecimal minValue;
  private Long num;

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BalanceDto() {
    super();
  }

  public BalanceDto(UUID id, AccountType type, String name, String currencyCode, BigDecimal value) {
    super(id, type, name, Date.valueOf(LocalDate.now()), false);
    this.currencyCode = currencyCode;
    this.value = value;
  }

  public String getCurrencyCode() {
    return currencyCode != null ?  currencyCode : "RUB";
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public String getCurrencySymbol() {
    return currencySymbol;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setCurrencySymbol(String currencySymbol) {
    this.currencySymbol = currencySymbol;
  }

  private Currency getCurrency() {
    assert currencyCode != null : this.toString();
    return Currency.getInstance(currencyCode);
  }

  public BigDecimal getValue() {
    return nvl(value, BigDecimal.ZERO.setScale(getCurrency().getDefaultFractionDigits(), 0));
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  public UUID getReserveId() {
    return reserveId;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setReserveId(UUID reserveId) {
    this.reserveId = reserveId;
  }

  public BigDecimal getCreditLimit() {
    return nvl(creditLimit, BigDecimal.ZERO.setScale(getCurrency().getDefaultFractionDigits(), 0));
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setCreditLimit(BigDecimal creditLimit) {
    this.creditLimit = creditLimit;
  }

  public BigDecimal getMinValue() {
    return nvl(minValue, BigDecimal.ZERO.setScale(getCurrency().getDefaultFractionDigits(), 0));
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setMinValue(BigDecimal minValue) {
    this.minValue = minValue;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public Long getNum() {
    return num != null ? num : 0L;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setNum(Long num) {
    this.num = num;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BigDecimal getFreeFunds() {
    return getValue().add(getCreditLimit().subtract(getMinValue()));
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
