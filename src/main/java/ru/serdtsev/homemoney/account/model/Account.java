package ru.serdtsev.homemoney.account.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Date;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Inheritance(strategy = InheritanceType.JOINED)
public class Account implements Serializable {
  @Id
  protected UUID id;

  @ManyToOne
  @JoinColumn(name = "balance_sheet_id")
  protected BalanceSheet balanceSheet;

  @Column(name = "created_date")
  private Date created;

  @Enumerated(EnumType.STRING)
  private AccountType type;

  private String name;

  @Column(name = "is_arc")
  private Boolean isArc;

  protected Account() {
  }

  public Account(BalanceSheet balanceSheet, AccountType type, String name, Date created, Boolean isArc) {
    this.id = UUID.randomUUID();
    this.balanceSheet = balanceSheet;
    this.created = created;
    this.type = type;
    this.name = name;
    this.isArc = isArc;
  }

  public void init() {
    this.id = UUID.randomUUID();
  }

  public void merge(Account account) {
    setType(account.getType());
    setName(account.getName());
    setCreated(account.getCreated());
    setArc(account.getArc());
  }

  public UUID getId() {
    return id;
  }

  public void setBalanceSheet(BalanceSheet balanceSheet) {
    this.balanceSheet = balanceSheet;
  }

  @JsonIgnore
  public BalanceSheet getBalanceSheet() {
    return balanceSheet;
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

  @JsonProperty("createdDate")
  public Date getCreated() {
    return created;
  }

  @JsonProperty("createdDate")
  public void setCreated(Date created) {
    this.created = created;
  }

  @JsonProperty("isArc")
  public Boolean getArc() {
    return isArc;
  }

  @JsonProperty("isArc")
  public void setArc(Boolean arc) {
    isArc = arc;
  }

  @JsonIgnore
  public String getSortIndex() {
    return getName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Account account = (Account) o;
    return Objects.equals(id, account.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}