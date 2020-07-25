package ru.serdtsev.homemoney.user;

import java.util.UUID;

public class Authentication {
  private UUID userId;
  private UUID bsId;
  private UUID token;

  public Authentication(UUID userId, UUID bsId, UUID token) {
    this.userId = userId;
    this.bsId = bsId;
    this.token = token;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public UUID getUserId() {
    return userId;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public UUID getBsId() {
    return bsId;
  }

  public void setBsId(UUID bsId) {
    this.bsId = bsId;
  }

  public UUID getToken() {
    return token;
  }

  public void setToken(UUID token) {
    this.token = token;
  }
}
