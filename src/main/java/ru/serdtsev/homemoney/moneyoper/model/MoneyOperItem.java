package ru.serdtsev.homemoney.moneyoper.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.serdtsev.homemoney.account.Balance;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "money_oper_item")
@NamedQuery(name = "MoneyOperItem.findByBalanceSheetAndValueOrderByPerformedDesc",
  query = "select m from MoneyOperItem m where balanceSheet = ?1 and abs(value) = ?2 order by performed desc")
public class MoneyOperItem implements Serializable {
  @Id
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "bs_id")
  @Nonnull
  private BalanceSheet balanceSheet;

  @ManyToOne
  @JoinColumn(name = "oper_id")
  @Nonnull
  private MoneyOper moneyOper;

  @ManyToOne
  @JoinColumn(name = "balance_id")
  @Nonnull
  private Balance balance;

  @Nonnull
  private BigDecimal value;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @Nonnull
  private LocalDate performed;

  private int index;

  @SuppressWarnings("unused")
  public MoneyOperItem() {
  }

  MoneyOperItem(UUID id, MoneyOper moneyOper, Balance balance, BigDecimal value, LocalDate performed, int index) {
    this.id = id;
    this.moneyOper = moneyOper;
    this.balanceSheet = moneyOper.getBalanceSheet();
    this.balance = balance;
    this.value = value;
    this.performed = performed;
    this.index = index;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public BalanceSheet getBalanceSheet() {
    return balanceSheet;
  }

  public void setBalanceSheet(BalanceSheet balanceSheet) {
    this.balanceSheet = balanceSheet;
  }

  @JsonIgnore
  public MoneyOper getMoneyOper() {
    return moneyOper;
  }

  public UUID getOperId() {
    return moneyOper.getId();
  }

  @JsonIgnore
  public Balance getBalance() {
    return balance;
  }

  public UUID getBalanceId() {
    return balance.getId();
  }

  public void setBalance(Balance balance) {
    this.balance = balance;
  }

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  @JsonProperty
  public LocalDate getPerformed() {
    return performed;
  }

  public void setPerformed(LocalDate performed) {
    this.performed = performed;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MoneyOperItem)) return false;
    MoneyOperItem that = (MoneyOperItem) o;
    return Objects.equals(moneyOper.getId(), that.moneyOper.getId()) &&
        Objects.equals(balance.getId(), that.balance.getId()) &&
        value.compareTo(that.value) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(moneyOper, balance, value);
  }

  public boolean essentialEquals(MoneyOperItem other) {
    assert equals(other);
    return moneyOper.equals(other.getMoneyOper())
        && balance.equals(other.getBalance()) && value.compareTo(other.getValue()) == 0;
  }

  @Override
  public String toString() {
    return "MoneyOperItem{" +
        "id=" + id +
        ", moneyOperId=" + moneyOper.getId() +
        ", balanceId=" + balance.getId() +
        ", value=" + value +
        ", performed=" + performed +
        ", index=" + index +
        '}';
  }
}
