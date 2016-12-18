package ru.serdtsev.homemoney.dto;

public class HmCurrency {
  private String currencyCode;
  private String displayName;
  private String symbol;

  public HmCurrency(String currencyCode, String displayName, String symbol) {
    this.currencyCode = currencyCode;
    this.displayName = displayName;
    this.symbol = symbol;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public String getDisplayName() {
    return displayName;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public String getSymbol() {
    return symbol;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }
}
