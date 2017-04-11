package ru.serdtsev.homemoney.balancesheet;

import ru.serdtsev.homemoney.account.*;
import ru.serdtsev.homemoney.dto.BalanceSheetDto;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "balance_sheets")
public class BalanceSheet {
  @Id
  private UUID id;

  private Timestamp created;

  @Column(name = "currency_code")
  private String currencyCode;

  @OneToOne
  @JoinColumn(name = "svc_rsv_id", insertable = false)
  private ServiceAccount svcRsv;

  @OneToOne
  @JoinColumn(name = "uncat_costs_id", insertable = false)
  private Category uncatCosts;

  @OneToOne
  @JoinColumn(name = "uncat_income_id", insertable = false)
  private Category uncatIncome;

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

  public static BalanceSheet fromDto(BalanceSheetDto dto) {
    return new BalanceSheet(dto.getId(), dto.getCreatedTs(), dto.getCurrencyCode());
  }

  public BalanceSheetDto toDto() {
    return new BalanceSheetDto(id, created, svcRsv.getId(), uncatCosts.getId(), uncatIncome.getId(), currencyCode);
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

  public ServiceAccount getSvcRsv() {
    return svcRsv;
  }

  public Category getUncatCosts() {
    return uncatCosts;
  }

  public Category getUncatIncome() {
    return uncatIncome;
  }

  public List<Account> getAccounts() {
    return this.accounts;
  }

  public List<Balance> getBalances() {
    return this.accounts.stream()
        .filter(account -> account instanceof Balance)
        .map(account -> (Balance) account)
        .collect(Collectors.toList());
  }

}
