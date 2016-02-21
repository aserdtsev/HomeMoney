package ru.serdtsev.homemoney.dto;

import java.io.Serializable;
import java.util.UUID;

public class Authentication implements Serializable {
  public UUID userId;
  public UUID bsId;
  public UUID token;

  public Authentication(UUID userId, UUID bsId, UUID token) {
    this.userId = userId;
    this.bsId = bsId;
    this.token = token;
  }
}
