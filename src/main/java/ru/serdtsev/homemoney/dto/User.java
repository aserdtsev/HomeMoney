package ru.serdtsev.homemoney.dto;

import java.util.UUID;

public class User {
  private UUID userId;
  private String email;
  private String pwdHash;
  private UUID bsId;

  public User() {
  }

  public UUID getUserId() {
    return userId;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPwdHash() {
    return pwdHash;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setPwdHash(String pwdHash) {
    this.pwdHash = pwdHash;
  }

  public UUID getBsId() {
    return bsId;
  }

  public void setBsId(UUID bsId) {
    this.bsId = bsId;
  }
}
