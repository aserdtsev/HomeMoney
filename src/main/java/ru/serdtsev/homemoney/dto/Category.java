package ru.serdtsev.homemoney.dto;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public class Category extends Account implements java.io.Serializable {
  private Optional<UUID> rootId;
  public Category() {
    setRootId(null);
  }
  public UUID getRootId() {
    return rootId.orElse(null);
  }
  public void setRootId(UUID rootId) {
    this.rootId = Optional.ofNullable(rootId);
  }
}
