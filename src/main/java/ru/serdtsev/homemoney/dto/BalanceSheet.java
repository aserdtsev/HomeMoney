package ru.serdtsev.homemoney.dto;

import java.sql.Timestamp;
import java.util.UUID;

public class BalanceSheet implements java.io.Serializable {
  public UUID id;
  public java.sql.Timestamp createdTs;
  public UUID svcRsvId;
  public UUID uncatCostsId;
  public UUID uncatIncomeId;

  public BalanceSheet() {
  }

  public String getId() {
    return id.toString();
  }

  public void setId(String id) {
    this.id = UUID.fromString(id);
  }

  public Timestamp getCreatedTs() {
    return createdTs;
  }

  public void setCreatedTs(Timestamp createdTs) {
    this.createdTs = createdTs;
  }

  public String getSvcRsvId() {
    return svcRsvId.toString();
  }

  public void setSvcRsvId(String svcRsvId) {
    this.svcRsvId = UUID.fromString(svcRsvId);
  }

  public String getUncatCostsId() {
    return uncatCostsId.toString();
  }

  public void setUncatCostsId(String uncatCostsId) {
    this.uncatCostsId = UUID.fromString(uncatCostsId);
  }

  public String getUncatIncomeId() {
    return uncatIncomeId.toString();
  }

  public void setUncatIncomeId(String uncatIncomeId) {
    this.uncatIncomeId = UUID.fromString(uncatIncomeId);
  }
}
