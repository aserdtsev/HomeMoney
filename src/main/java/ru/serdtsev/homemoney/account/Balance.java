package ru.serdtsev.homemoney.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.dto.MoneyTrn;
import ru.serdtsev.homemoney.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Date;
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
  private ReserveRepository reserveRepo;

  @Column(name = "currency_code")
  private String currencyCode;
  
  private BigDecimal value;

  @OneToOne
  @JoinColumn(name = "reserve_id")
  private Reserve reserve;

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

  private BigDecimal getCreditLimit() {
    return nvl(creditLimit, BigDecimal.ZERO.setScale(getCurrency().getDefaultFractionDigits(), 0));
  }

  public void setCreditLimit(BigDecimal creditLimit) {
    this.creditLimit = creditLimit;
  }

  private BigDecimal getMinValue() {
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
    reserve = reserveId != null ? reserveRepo.findOne(reserveId) : null;
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
