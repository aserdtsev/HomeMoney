package ru.serdtsev.homemoney.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class CategoryStat {
  private UUID id;
  private UUID rootId;
  private String name;
  private BigDecimal amount;

  public CategoryStat() {
  }

  public CategoryStat(UUID id, UUID rootId, String name, BigDecimal amount) {
    this.id = id;
    this.rootId = rootId;
    this.name = name;
    this.amount = amount;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getRootId() {
    return rootId;
  }

  public void setRootId(UUID rootId) {
    this.rootId = rootId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
}
