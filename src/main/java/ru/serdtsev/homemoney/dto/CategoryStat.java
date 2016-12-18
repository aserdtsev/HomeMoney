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

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getRootId() {
    return rootId;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
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
