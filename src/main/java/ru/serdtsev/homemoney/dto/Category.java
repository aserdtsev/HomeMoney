package ru.serdtsev.homemoney.dto;

import java.util.UUID;

public class Category extends Account {
  private UUID rootId;

  public Category() {
    this(null);
  }

  public Category(UUID rootId) {
    this.rootId = rootId;
  }

  public UUID getRootId() {
    return rootId;
  }

  public void setRootId(UUID rootId) {
    this.rootId = rootId;
  }
}
