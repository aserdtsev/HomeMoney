package ru.serdtsev.homemoney.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.dao.MoneyTrnsDao;
import ru.serdtsev.homemoney.dto.MoneyTrn;
import ru.serdtsev.homemoney.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static ru.serdtsev.homemoney.utils.Utils.nvl;

@Entity
@Table(name = "balances")
@DiscriminatorValue("balance")
public class Balance extends Account {
  private static Logger log = LoggerFactory.getLogger(Balance.class);

  @SuppressWarnings("unused")
  @Transient
  @Autowired
  private AccountRepository accountRepo;

  @SuppressWarnings("unused")
  @Transient
  @Autowired
  private MoneyTrnsDao moneyTrnsDao;

  @SuppressWarnings("unused")
  @Transient
  @Autowired
  private ReserveRepository reserveRepo;

  @Column(name = "currency_code")
  private String currencyCode;
  
  private BigDecimal value;

  @OneToOne
  @JoinColumn(name = "reserve_id")
  private Reserve reserve;

  @Transient
  private UUID reserveId;

  @Column(name = "credit_limit")
  private BigDecimal creditLimit;

  @Column(name = "min_value")
  private BigDecimal minValue;

  private Long num;

  Balance() {
    super();
  }

  public Balance(BalanceSheet balanceSheet, AccountType type, String name, Date created, Boolean isArc, String currencyCode,
      BigDecimal value, BigDecimal minValue) {
    super(balanceSheet, type, name, created, isArc);
    this.currencyCode = currencyCode;
    this.value = value;
    this.minValue = minValue;
  }

  @Override
  public void init() {
    super.init();
    value = nvl(value, BigDecimal.ZERO);
    creditLimit = nvl(creditLimit, BigDecimal.ZERO);
    minValue = nvl(minValue, BigDecimal.ZERO);
    num = nvl(num, 0L);
    // todo Reserve!
  }

  public void merge(Balance balance, ReserveRepository reserveRepo, MoneyTrnsDao moneyTrnsDao) {
    super.merge(balance);
    setCreditLimit(balance.getCreditLimit());
    setMinValue(balance.getMinValue());
    setReserve(balance.getReserveId() != null ? reserveRepo.findOne(balance.getReserveId()) : null);
    if (balance.getValue().compareTo(getValue()) != 0) {
      BalanceSheet bs = getBalanceSheet();
      boolean more = balance.getValue().compareTo(getValue()) == 1;
      UUID fromAccId = more ? bs.getUncatIncome().getId() : balance.getId();
      UUID toAccId = more ? balance.getId() : bs.getUncatCosts().getId();
      BigDecimal amount = balance.getValue().subtract(getValue()).abs();
      MoneyTrn moneyTrn = new MoneyTrn(UUID.randomUUID(), MoneyTrn.Status.done, java.sql.Date.valueOf(LocalDate.now()),
          fromAccId, toAccId, amount, MoneyTrn.Period.single, "корректировка остатка");
      moneyTrnsDao.createMoneyTrn(bs.getId(), moneyTrn);
      // todo После полного перехода на JPA обновлять баланс здесь будет не нужно - он будет обновлен при проводке операции.
      balance.setValue(balance.getValue());
    }
  }

  public String getCurrencyCode() {
    return currencyCode != null ?  currencyCode : "RUB";
  }

  public String getCurrencySymbol() {
    return getCurrency().getSymbol();
  }

  private Currency getCurrency() {
    assert currencyCode != null : this.toString();
    return Currency.getInstance(currencyCode);
  }

  public BigDecimal getValue() {
    return nvl(value, BigDecimal.ZERO.setScale(getCurrency().getDefaultFractionDigits(), 0));
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  public void changeValue(BigDecimal amount, UUID trnId, MoneyTrn.Status status) {
    BigDecimal beforeValue = value.plus();
    value = value.add(amount);
    log.info("Balance value changed; " +
        "id: " + getId() + ", " +
        "trnId: " + trnId + ", " +
        "status: " + status.name() + ", " +
        "before: " + beforeValue + ", " +
        "after: " + value + ".");
  }

  public BigDecimal getCreditLimit() {
    return nvl(creditLimit, BigDecimal.ZERO.setScale(getCurrency().getDefaultFractionDigits(), 0));
  }

  public void setCreditLimit(BigDecimal creditLimit) {
    this.creditLimit = creditLimit;
  }

  public BigDecimal getMinValue() {
    return nvl(minValue, BigDecimal.ZERO.setScale(getCurrency().getDefaultFractionDigits(), 0));
  }

  public void setMinValue(BigDecimal minValue) {
    this.minValue = minValue;
  }

  @JsonIgnore
  public Reserve getReserve() {
    return reserve;
  }

  /**
   * Для сериализации в JSON.
   */
  @SuppressWarnings("unused")
  public UUID getReserveId() {
    return reserveId;
  }

  /**
   * Для десериализации из JSON.
   */
  @SuppressWarnings("unused")
  public void setReserveId(@Nullable UUID reserveId) {
    this.reserveId = reserveId;
  }

  @Nonnull
  public Long getNum() {
    return Utils.nvl(num, 0L);
  }

  public void setNum(Long num) {
    this.num = num;
  }

  public void setReserve(Reserve reserve) {
    this.reserve = reserve;
  }

  @SuppressWarnings("unused")
  public BigDecimal getFreeFunds() {
    return getValue().add(getCreditLimit().subtract(getMinValue()));
  }

}
