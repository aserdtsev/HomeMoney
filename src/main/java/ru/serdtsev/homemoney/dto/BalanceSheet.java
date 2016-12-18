package ru.serdtsev.homemoney.dto;

import java.sql.Timestamp;
import java.util.UUID;

public class BalanceSheet {
  private UUID id;
  private String defaultCurrencyCode;
  private Timestamp createdTs;
  private UUID svcRsvId;
  private UUID uncatCostsId;
  private UUID uncatIncomeId;
  private String currencyCode;

  @SuppressWarnings({"unused", "WeakerAccess"})
  public BalanceSheet() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public String getDefaultCurrencyCode() {
    return defaultCurrencyCode;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setDefaultCurrencyCode(String defaultCurrencyCode) {
    this.defaultCurrencyCode = defaultCurrencyCode;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public Timestamp getCreatedTs() {
    return createdTs;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setCreatedTs(Timestamp createdTs) {
    this.createdTs = createdTs;
  }

  public UUID getSvcRsvId() {
    return svcRsvId;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setSvcRsvId(UUID svcRsvId) {
    this.svcRsvId = svcRsvId;
  }

  public UUID getUncatCostsId() {
    return uncatCostsId;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setUncatCostsId(UUID uncatCostsId) {
    this.uncatCostsId = uncatCostsId;
  }

  public UUID getUncatIncomeId() {
    return uncatIncomeId;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setUncatIncomeId(UUID uncatIncomeId) {
    this.uncatIncomeId = uncatIncomeId;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }
}
