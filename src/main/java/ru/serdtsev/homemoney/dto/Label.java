package ru.serdtsev.homemoney.dto;

import java.util.UUID;

public class Label {
  private UUID id;
  private String name;

  @SuppressWarnings("unused")
  public Label() {
  }

  public Label(UUID id, String name) {
    this.id = id;
    this.name = name;
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


}
