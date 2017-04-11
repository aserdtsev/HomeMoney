package ru.serdtsev.homemoney.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.serdtsev.homemoney.utils.Utils;

import java.sql.Date;
import java.util.UUID;

public class AccountDto {
  private UUID id;
  private AccountType type;
  private String name;
  private java.sql.Date createdDate;
  private Boolean isArc;

  public AccountDto() {
  }

  public AccountDto(UUID id, AccountType type, String name, Date createdDate, Boolean isArc) {
    this.id = id;
    this.type = type;
    this.name = name;
    this.createdDate = createdDate;
    this.isArc = isArc;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public AccountType getType() {
    return type;
  }

  public void setType(AccountType type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public Boolean getIsArc() {
    return Utils.nvl(isArc, false);
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public void setIsArc(Boolean isArc) {
    this.isArc = isArc;
  }

  @JsonIgnore
  public Boolean isBalance() {
    return AccountType.debit == type || AccountType.credit == type || AccountType.reserve == type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AccountDto account = (AccountDto) o;

    return getId() != null ? getId().equals(account.getId()) : account.getId() == null;
  }

  @Override
  public int hashCode() {
    return getId() != null ? getId().hashCode() : 0;
  }

  @Override
  public String toString() {
    return "Account{" +
        "id=" + id +
        ", type=" + type +
        ", name='" + name + '\'' +
        ", createdDate=" + createdDate +
        ", isArc=" + isArc +
        '}';
  }
}
