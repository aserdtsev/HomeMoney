package ru.serdtsev.homemoney.user;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "auth_tokens")
public class UserAuthToken {
  @Id
  private UUID token;

  private UUID userId;

  UserAuthToken() {
  }

  public UserAuthToken(UUID token, UUID userId) {
    this.token = token;
    this.userId = userId;
  }

  public UUID getUserId() {
    return userId;
  }
}
