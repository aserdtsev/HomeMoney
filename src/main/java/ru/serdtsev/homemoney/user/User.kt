package ru.serdtsev.homemoney.user;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
  @Id
  @Column(name = "user_id")
  private UUID userId;

  @Column(length = 100)
  private String email;

  @Column(name = "pwd_hash", length = 50)
  private String pwdHash;

  @Column(name = "bs_id")
  private UUID bsId;

  @SuppressWarnings({"unused", "WeakerAccess"})
  private User() {
  }

  public User(UUID id, UUID bsId, String email, String pwdHash) {
    this.userId = id;
    this.email = email;
    this.pwdHash = pwdHash;
    this.bsId = bsId;
  }

  public UUID getId() {
    return userId;
  }

  public UUID getBsId() {
    return bsId;
  }

  public String getPwdHash() {
    return pwdHash;
  }
}
