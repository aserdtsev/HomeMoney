package ru.serdtsev.homemoney.dto;

import java.util.UUID;

public class Label {
  private UUID id;
  private String name;
  private UUID rootId;
  private Boolean isCategory;
  private Boolean isArc;

  @SuppressWarnings("unused")
  public Label() {
  }

  public Label(UUID id, String name, UUID rootId, Boolean isCategory, Boolean isArc) {
    this.id = id;
    this.name = name;
    this.rootId = rootId;
    this.isCategory = isCategory;
    this.isArc = isArc;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public UUID getRootId() {
    return rootId;
  }

  public void setRootId(UUID rootId) {
    this.rootId = rootId;
  }

  public Boolean getIsCategory() {
    return isCategory;
  }

  public void setIsCategory(Boolean category) {
    isCategory = category;
  }

  public Boolean getArc() {
    return isArc;
  }

  public void setArc(Boolean arc) {
    isArc = arc;
  }
}
