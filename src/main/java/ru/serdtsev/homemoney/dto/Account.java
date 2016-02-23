package ru.serdtsev.homemoney.dto;

import javax.xml.bind.annotation.XmlTransient;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class Account {
  public enum Type { debit, credit, expense, income, reserve, asset, service }
  private UUID id;
  private String name;
  private Type type;
  private Date createdDate;
  private Boolean isArc;

  public Account() {
  }

  public Account(Category category) {
    this(category.getId(), category.getType(), category.getName());
  }

  public Account(UUID id, Type type, String name) {
    setId(id);
    setName(name);
    setType(type);
  }

  public UUID getId() {
    return this.id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Type getType() {
    return this.type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public Date getCreatedDate() {
    return Optional.ofNullable(createdDate).orElse(Date.valueOf(LocalDate.now()));
  }

  @SuppressWarnings("unused")
  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public Boolean getIsArc() {
    return Optional.ofNullable(isArc).orElse(false);
  }

  public void setIsArc(Boolean isArc) {
    this.isArc = isArc;
  }

  @XmlTransient
  public Boolean isBalance() {
    return Type.debit.equals(getType())
        || Type.credit.equals(getType())
        || Type.reserve.equals(getType());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Account)) return false;
    Account that = (Account) o;
    return id.equals(that.getId());
  }

  @Override
  public int hashCode() {
    return Optional.ofNullable(id).isPresent() ? id.hashCode() : 0;
  }
}
