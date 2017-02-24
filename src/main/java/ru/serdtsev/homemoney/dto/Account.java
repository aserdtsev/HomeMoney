package ru.serdtsev.homemoney.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.serdtsev.homemoney.utils.Utils;

import java.sql.Date;
import java.time.LocalDate;
import java.util.UUID;

public class Account {
  public enum Type {
    debit, credit, expense, income, reserve, asset, service
  }

  private UUID id;
  private Type type;
  private String name;
  private java.sql.Date createdDate;
  private Boolean isArc;

  public Account() {
    createdDate = java.sql.Date.valueOf(LocalDate.now());
  }

  public Account(UUID id, Type type, String name) {
    this();
    this.id = id;
    this.type = type;
    this.name = name;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
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
    return Type.debit == type || Type.credit == type || Type.reserve == type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Account account = (Account) o;

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
