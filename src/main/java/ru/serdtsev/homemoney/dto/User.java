package ru.serdtsev.homemoney.dto;

import java.io.Serializable;
import java.util.UUID;

public class User implements Serializable {
  public UUID userId;
  public String email;
  public String pwdHash;
  public UUID bsId;

  public User() {}

  public User(UUID userId, String email, String pwdHash, UUID bsId) {
    this.userId = userId;
    this.email = email;
    this.pwdHash = pwdHash;
    this.bsId = bsId;
  }

  public String getUserId() {
    return userId.toString();
  }

  public void setUserId(String userId) {
    this.userId = UUID.fromString(userId);
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

  public void setPwdHash(String pwdHash) {
    this.pwdHash = pwdHash;
  }

  public String getBsId() {
    return bsId.toString();
  }

  public void setBsId(String bsId) {
    this.bsId = UUID.fromString(bsId);
  }
}
