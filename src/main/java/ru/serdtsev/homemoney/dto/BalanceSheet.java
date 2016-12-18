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

  public BalanceSheet() {
  }

  public BalanceSheet(UUID id, String defaultCurrencyCode, Timestamp createdTs, UUID svcRsvId, UUID uncatCostsId,
      UUID uncatIncomeId, String currencyCode) {
    this.id = id;
    this.defaultCurrencyCode = defaultCurrencyCode;
    this.createdTs = createdTs;
    this.svcRsvId = svcRsvId;
    this.uncatCostsId = uncatCostsId;
    this.uncatIncomeId = uncatIncomeId;
    this.currencyCode = currencyCode;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getDefaultCurrencyCode() {
    return defaultCurrencyCode;
  }

  public void setDefaultCurrencyCode(String defaultCurrencyCode) {
    this.defaultCurrencyCode = defaultCurrencyCode;
  }

  public Timestamp getCreatedTs() {
    return createdTs;
  }

  public void setCreatedTs(Timestamp createdTs) {
    this.createdTs = createdTs;
  }

  public UUID getSvcRsvId() {
    return svcRsvId;
  }

  public void setSvcRsvId(UUID svcRsvId) {
    this.svcRsvId = svcRsvId;
  }

  public UUID getUncatCostsId() {
    return uncatCostsId;
  }

  public void setUncatCostsId(UUID uncatCostsId) {
    this.uncatCostsId = uncatCostsId;
  }

  public UUID getUncatIncomeId() {
    return uncatIncomeId;
  }

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
