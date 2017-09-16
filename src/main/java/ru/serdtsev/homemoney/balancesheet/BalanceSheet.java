package ru.serdtsev.homemoney.balancesheet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.serdtsev.homemoney.account.*;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "balance_sheets")
public class BalanceSheet implements Serializable {
  @Id
  private UUID id;

  private Timestamp created;

  @Column(name = "currency_code")
  private String currencyCode;

  @OneToOne
  @JoinColumn(name = "svc_rsv_id", insertable = false)
  private ServiceAccount svcRsv;

  @Transient
  private UUID svcRsvId;

  @OneToOne
  @JoinColumn(name = "uncat_costs_id", insertable = false)
  private Category uncatCosts;

  @Transient
  private UUID uncatCostsId;

  @OneToOne
  @JoinColumn(name = "uncat_income_id", insertable = false)
  private Category uncatIncome;

  @Transient
  private UUID uncatIncomeId;

  @OneToMany
  @JoinColumn(name = "balance_sheet_id")
  private List<Account> accounts;

  @SuppressWarnings({"unused", "WeakerAccess"})
  protected BalanceSheet() { }

  private BalanceSheet(UUID id, Timestamp created, String currencyCode) {
    this(id, created, currencyCode, null, null, null);
  }

  private BalanceSheet(UUID id, Timestamp created, String currencyCode, @Nullable ServiceAccount svcRsv,
      @Nullable Category uncatCosts, @Nullable Category uncatIncome) {
    this.id = id;
    this.created = created;
    this.currencyCode = currencyCode;
    this.svcRsv = svcRsv;
    this.uncatCosts = uncatCosts;
    this.uncatIncome = uncatIncome;
  }

  public UUID getId() {
    return id;
  }

  public static BalanceSheet newInstance() {
    return new BalanceSheet(UUID.randomUUID(), Timestamp.valueOf(LocalDateTime.now()), "RUB")
        .init();
  }

  public BalanceSheet init() {
    Date now = Date.valueOf(LocalDate.now());
    svcRsv = new ServiceAccount(this, "Service reserve", now, false);
    uncatCosts = new Category(this, AccountType.expense, "<Без категории>", now, false, null);
    uncatIncome = new Category(this, AccountType.income, "<Без категории>", now, false, null);
    return this;
  }


  public String getCurrencyCode() {
    return currencyCode;
  }

  @JsonIgnore
  public ServiceAccount getSvcRsv() {
    return svcRsv;
  }

  public UUID getSvcRsvId() {
    return svcRsv.getId();
  }

  public void setSvcRsvId(UUID svcRsvId) {
    this.svcRsvId = svcRsvId;
  }

  @JsonIgnore
  public Category getUncatCosts() {
    return uncatCosts;
  }

  public UUID getUncatCostsId() {
    return uncatCosts.getId();
  }

  public void setUncatCostsId(UUID uncatCostsId) {
    this.uncatCostsId = uncatCostsId;
  }

  @JsonIgnore
  public Category getUncatIncome() {
    return uncatIncome;
  }

  public UUID getUncatIncomeId() {
    return uncatIncome.getId();
  }

  public void setUncatIncomeId(UUID uncatIncomeId) {
    this.uncatIncomeId = uncatIncomeId;
  }

  @JsonIgnore
  public List<Account> getAccounts() {
    return this.accounts;
  }

  @JsonIgnore
  public List<Balance> getBalances() {
    return this.accounts.stream()
        .filter(account -> account instanceof Balance)
        .map(account -> (Balance) account)
        .collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return "BalanceSheet{" +
        "id=" + id +
        ", created=" + created +
        ", currencyCode='" + currencyCode + '\'' +
        '}';
  }
}
