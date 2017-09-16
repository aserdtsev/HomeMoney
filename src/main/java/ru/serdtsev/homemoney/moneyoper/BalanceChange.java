package ru.serdtsev.homemoney.moneyoper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.serdtsev.homemoney.account.Balance;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "balance_changes")
public class BalanceChange implements Serializable {
  @Id
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "oper_id")
  private MoneyOper moneyOper;

  @ManyToOne
  @JoinColumn(name = "balance_id")
  private Balance balance;

  private BigDecimal value;

  @Column(name = "made")
  private Date performed;

  private int index;

  @SuppressWarnings("unused")
  public BalanceChange() {
  }

  BalanceChange(UUID id, MoneyOper moneyOper, Balance balance, BigDecimal value, Date performed, int index) {
    this.id = id;
    this.moneyOper = moneyOper;
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

  public Date getPerformed() {
    return performed;
  }

  public void setPerformed(Date performed) {
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
    if (!(o instanceof BalanceChange)) return false;
    BalanceChange that = (BalanceChange) o;
    return Objects.equals(moneyOper.getId(), that.moneyOper.getId()) &&
        Objects.equals(balance.getId(), that.balance.getId()) &&
        value.compareTo(that.value) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(moneyOper, balance, value);
  }

  public boolean essentialEquals(BalanceChange other) {
    assert equals(other);
    return moneyOper.equals(other.getMoneyOper())
        && balance.equals(other.getBalance()) && value.compareTo(other.getValue()) == 0;
  }

  @Override
  public String toString() {
    return "BalanceChange{" +
        "id=" + id +
        ", moneyOperId=" + moneyOper.getId() +
        ", balanceId=" + balance.getId() +
        ", value=" + value +
        ", performed=" + performed +
        ", index=" + index +
        '}';
  }
}
