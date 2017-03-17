package ru.serdtsev.homemoney.account;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

@Entity
public class Account {
  @Id
  private UUID id;
}
