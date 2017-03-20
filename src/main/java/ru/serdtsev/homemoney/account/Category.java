package ru.serdtsev.homemoney.account;

import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import javax.persistence.*;
import java.sql.Date;

@Entity
@Table(name = "categories")
@DiscriminatorValue("category")
public class Category extends Account {
  @ManyToOne
  @JoinColumn(name = "root_id")
  private Category root;

  protected Category() {
  }

  public Category(BalanceSheet balanceSheet, AccountType type, String name, Date created, Boolean isArc, Category root) {
    super(balanceSheet, type, name, created, isArc);
    this.root = root;
  }
}
