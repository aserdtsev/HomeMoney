package ru.serdtsev.homemoney.dto;

import javax.xml.bind.annotation.XmlTransient;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class Account implements java.io.Serializable {
  public enum Type { debit, credit, expense, income, reserve, asset, service }
  private Optional<UUID> id;
  private Optional<String> name;
  private Optional<Type> type;
  private Optional<Date> createdDate = Optional.empty();
  private Optional<Boolean> isArc = Optional.empty();

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
    return this.id.get();
  }

  public void setId(UUID id) {
    this.id = Optional.of(id);
  }

  public String getName() {
    return name.get();
  }

  public void setName(String name) {
    this.name = Optional.of(name);
  }

  public Type getType() {
    return this.type.get();
  }

  public void setType(Type type) {
    this.type = Optional.of(type);
  }

  public Date getCreatedDate() {
    return createdDate.orElse(Date.valueOf(LocalDate.now()));
  }

  @SuppressWarnings("unused")
  public void setCreatedDate(Date createdDate) {
    this.createdDate = Optional.ofNullable(createdDate);
  }

  public Boolean getIsArc() {
    return isArc.orElse(false);
  }

  public void setIsArc(Boolean isArc) {
    this.isArc = Optional.of(isArc);
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
    return id.get().equals(that.getId());
  }

  @Override
  public int hashCode() {
    return id.isPresent() ? id.get().hashCode() : 0;
  }
}
