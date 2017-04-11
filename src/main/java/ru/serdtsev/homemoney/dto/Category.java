package ru.serdtsev.homemoney.dto;

import ru.serdtsev.homemoney.account.AccountDto;

import java.util.UUID;

public class Category extends AccountDto {
  private UUID rootId;

  @SuppressWarnings({"unused", "WeakerAccess"})
  public Category() {
  }

  public UUID getRootId() {
    return rootId;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setRootId(UUID rootId) {
    this.rootId = rootId;
  }
}
