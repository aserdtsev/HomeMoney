package ru.serdtsev.homemoney.account;

import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Date;

@Entity
@DiscriminatorValue("service")
@Table(name = "svc_accounts")
public class ServiceAccount extends Account {
  protected ServiceAccount() {
  }

  public ServiceAccount(BalanceSheet balanceSheet, String name, Date created, Boolean isArc) {
    super(balanceSheet, AccountType.service, name, created, isArc);
  }
}
