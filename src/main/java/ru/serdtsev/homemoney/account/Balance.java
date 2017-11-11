package ru.serdtsev.homemoney.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper;
import ru.serdtsev.homemoney.moneyoper.MoneyOperService;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus;
import ru.serdtsev.homemoney.moneyoper.model.Period;
import ru.serdtsev.homemoney.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.UUID;

import static ru.serdtsev.homemoney.utils.Utils.nvl;

@Entity
@Table(name = "balances")
@DiscriminatorValue("balance")
public class Balance extends Account {
  private static Logger log = LoggerFactory.getLogger(Balance.class);

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

  public void init(ReserveRepository reserveRepo) {
    super.init();
    value = nvl(value, BigDecimal.ZERO);
    creditLimit = nvl(creditLimit, BigDecimal.ZERO);
    minValue = nvl(minValue, BigDecimal.ZERO);
    num = nvl(num, 0L);
    if (reserveId != null) {
      reserve = reserveRepo.findOne(reserveId);
    }
  }

  public void merge(Balance balance, ReserveRepository reserveRepo, MoneyOperService moneyOperService) {
    super.merge(balance);
    setCreditLimit(balance.getCreditLimit());
    setMinValue(balance.getMinValue());
    setReserve(balance.getReserveId() != null ? reserveRepo.findOne(balance.getReserveId()) : null);
    if (balance.getValue().compareTo(getValue()) != 0) {
      BalanceSheet balanceSheet = getBalanceSheet();
      boolean more = balance.getValue().compareTo(getValue()) > 0;
      UUID fromAccId = more ? balanceSheet.getUncatIncome().getId() : balance.getId();
      UUID toAccId = more ? balance.getId() : balanceSheet.getUncatCosts().getId();
      BigDecimal amount = balance.getValue().subtract(getValue()).abs();

      if (balance.getType() == AccountType.reserve) {
        balance.setValue(balance.getValue());
      } else {
        MoneyOper moneyOper = moneyOperService.newMoneyOper(balanceSheet, UUID.randomUUID(), MoneyOperStatus.pending, java.sql.Date.valueOf(LocalDate.now()),
            0, new ArrayList<>(), "корректировка остатка", Period.single, fromAccId, toAccId, amount, amount,
            null, null);
        moneyOper.complete();
        moneyOperService.save(moneyOper);
      }
    }
  }

  public String getCurrencyCode() {
    return currencyCode != null ?  currencyCode : "RUB";
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
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

  @Deprecated
  public void changeValue(BigDecimal amount, UUID trnId, MoneyOperStatus status) {
    BigDecimal beforeValue = value.plus();
    value = value.add(amount);
    log.info("Balance value changed; " +
        "id: " + getId() + ", " +
        "trnId: " + trnId + ", " +
        "status: " + status.name() + ", " +
        "before: " + beforeValue + ", " +
        "after: " + value + ".");
  }

  public void changeValue(BigDecimal amount, MoneyOper oper) {
    BigDecimal beforeValue = value.plus();
    value = value.add(amount);
    log.info("Balance value changed; " +
        "id: " + getId() + ", " +
        "operId: " + oper.getId() + ", " +
        "status: " + oper.getStatus().name() + ", " +
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
    return reserve != null ? reserve.getId() : null;
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
