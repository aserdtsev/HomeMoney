package ru.serdtsev.homemoney.account;

import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import javax.persistence.*;
import java.sql.Date;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Inheritance(strategy = InheritanceType.JOINED)
public class Account {
  @Id
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "balance_sheet_id")
  private BalanceSheet balanceSheet;

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

  public UUID getId() {
    return id;
  }
}
