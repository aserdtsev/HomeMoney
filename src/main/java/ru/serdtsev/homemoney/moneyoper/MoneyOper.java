package ru.serdtsev.homemoney.moneyoper;

import org.apache.logging.log4j.util.Strings;
import ru.serdtsev.homemoney.account.Account;
import ru.serdtsev.homemoney.account.AccountRepository;
import ru.serdtsev.homemoney.account.AccountType;
import ru.serdtsev.homemoney.account.Balance;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.utils.Utils;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static ru.serdtsev.homemoney.moneyoper.MoneyOperStatus.*;
import static ru.serdtsev.homemoney.utils.Utils.assertNonNulls;

@Entity
@Table(name = "money_trns")
public class MoneyOper implements Serializable {
  @Id
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "balance_sheet_id")
  private BalanceSheet balanceSheet;

  @Enumerated(EnumType.STRING)
  private MoneyOperStatus status;

  @Column(name = "trn_date")
  private Date performed;

  @Column(name = "date_num")
  private Integer dateNum;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "oper_id")
  private List<BalanceChange> balanceChanges;

  @ManyToMany
  @JoinTable(name = "labels2objs",
      joinColumns = @JoinColumn(name = "obj_id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)),
      inverseJoinColumns = @JoinColumn(name = "label_id"))
  private Set<Label> labels;

  private String comment;

  @Enumerated(EnumType.STRING)
  private Period period;

  @Column(name = "created_ts")
  private Timestamp created;

  @OneToOne
  @JoinColumn(name = "parent_id")
  private MoneyOper parentOper;

  @Column(name = "from_acc_id")
  private UUID fromAccId;

  @Column(name = "to_acc_id")
  private UUID toAccId;

  @Column(name = "amount")
  private BigDecimal amount;

  @Column(name = "to_amount")
  private BigDecimal toAmount;

  /**
   * Идентификатор повторяющейся операции. Служит для получения списка операций, которые были созданы по одному шаблону.
   */
  @Column(name = "recurrence_id")
  private UUID recurrenceId;

  public MoneyOper() {
  }

  public MoneyOper(UUID id, BalanceSheet balanceSheet, MoneyOperStatus status, Date performed, Integer dateNum,
      Collection<Label> labels, String comment, Period period) {
    this.id = id;
    this.balanceSheet = balanceSheet;
    this.status = status;
    this.performed = performed;
    this.dateNum = dateNum;
    this.balanceChanges = new ArrayList<>();
    this.labels = new HashSet<>(labels);
    this.comment = comment;
    this.period = period;
    this.created = Timestamp.from(Instant.now());
  }

  public UUID getId() {
    return id;
  }

  public BalanceSheet getBalanceSheet() {
    return balanceSheet;
  }

  private void setStatus(MoneyOperStatus status) {
    this.status = status;
  }

  public MoneyOperStatus getStatus() {
    return status;
  }

  public Date getPerformed() {
    return performed;
  }

  public void setPerformed(Date performed) {
    this.performed = performed;
  }

  public List<BalanceChange> getBalanceChanges() {
    return balanceChanges;
  }

  public void setBalanceChanges(List<BalanceChange> balanceChanges) {
    this.balanceChanges = balanceChanges;
  }

  public Period getPeriod() {
    return period;
  }

  public void setPeriod(Period period) {
    this.period = period;
  }

  public String getComment() {
    return Utils.nvl(comment, Strings.EMPTY);
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Collection<Label> getLabels() {
    return Collections.unmodifiableSet(labels);
  }

  public void setLabels(Collection<Label> labels) {
    this.labels.retainAll(labels);
    this.labels.addAll(labels);
  }

  public Integer getDateNum() {
    return dateNum;
  }

  public void setDateNum(Integer dateNum) {
    this.dateNum = dateNum;
  }

  public void setParentOper(MoneyOper parentOper) {
    this.parentOper = parentOper;
  }

  public MoneyOper getParentOper() {
    return parentOper;
  }

  public UUID getParentOperId() {
    return parentOper != null ? parentOper.getId() : null;
  }

  public Timestamp getCreated() {
    return created;
  }

  public void setFromAccId(UUID fromAccId) {
    this.fromAccId = fromAccId;
  }

  public UUID getFromAccId() {
    return fromAccId;
  }

  public void setToAccId(UUID toAccId) {
    this.toAccId = toAccId;
  }

  public UUID getToAccId() {
    return toAccId;
  }

  void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public BigDecimal getAmount() {
    return balanceChanges.stream()
        .map(BalanceChange::getValue)
        .filter(value -> value.signum() < 0)
        .map(BigDecimal::abs)
        .findFirst()
        .orElse(amount);
  }

  public String getCurrencyCode() {
    return balanceChanges.stream()
        .sorted(Comparator.comparingInt(change -> change.getValue().signum()))
        .map(change -> change.getBalance().getCurrencyCode())
        .findFirst()
        .orElse(getBalanceSheet().getCurrencyCode());
  }

  public String getCurrencyCode(AccountRepository accountRepo) {
    Account account = accountRepo.findOne(getFromAccId());
    if (account instanceof Balance) {
      return ((Balance) account).getCurrencyCode();
    }
    return getBalanceSheet().getCurrencyCode();
  }

  void setToAmount(BigDecimal toAmount) {
    this.toAmount = toAmount;
  }

  public BigDecimal getToAmount() {
    return balanceChanges.stream()
        .map(BalanceChange::getValue)
        .filter(value -> value.signum() > 0)
        .map(BigDecimal::abs)
        .findFirst()
        .orElse(toAmount);
  }

  public String getToCurrencyCode() {
    return balanceChanges.stream()
        .sorted(Comparator.comparingInt(change -> change.getValue().signum() * -1))
        .map(change -> change.getBalance().getCurrencyCode())
        .findFirst()
        .orElse(getBalanceSheet().getCurrencyCode());
  }

  public String getToCurrencyCode(AccountRepository accountRepo) {
    Account account = accountRepo.findOne(getToAccId());
    if (account instanceof Balance) {
      return ((Balance) account).getCurrencyCode();
    }
    return getBalanceSheet().getCurrencyCode();
  }

  public UUID getRecurrenceId() {
    return recurrenceId;
  }

  public void setRecurrenceId(UUID recurrenceId) {
    this.recurrenceId = recurrenceId;
  }

  public MoneyOperType getType() {
    if (balanceChanges.stream().findFirst().orElseThrow(IllegalStateException::new).getBalance().getType() == AccountType.reserve) {
      return MoneyOperType.transfer;
    }
    Optional<Integer> valueSignumSumOpt = balanceChanges.stream()
        .map(change -> change.getValue().signum())
        .reduce((a, s) -> a += s);
    int valueSignumSum = valueSignumSumOpt.orElseThrow(() ->
        new IllegalStateException("balanceChanges does not have items: " + id));
    if (valueSignumSum > 0) {
      return MoneyOperType.income;
    } else if (valueSignumSum < 0) {
      return MoneyOperType.expense;
    }
    return MoneyOperType.transfer;
  }

  public MoneyOperType getType(AccountRepository accountRepo) {
    Account fromAcc = accountRepo.findOne(fromAccId);
    Account toAcc = accountRepo.findOne(toAccId);
    if (fromAcc.getType().equals(AccountType.income)) {
      return MoneyOperType.income;
    }
    if (toAcc.getType().equals(AccountType.expense)) {
      return MoneyOperType.expense;
    }
    return MoneyOperType.transfer;
  }

  public void complete() {
    assert getStatus() == pending || getStatus() == cancelled : getStatus();
    assert !getPerformed().toLocalDate().isAfter(LocalDate.now());
    changeBalances(false);
    setStatus(done);
  }

  public void cancel() {
    assert getStatus() == done || getStatus() == pending || getStatus() == template;
    if (getStatus() == done) {
      changeBalances(true);
    }
    setStatus(cancelled);
  }

  public void addBalanceChanges(Collection<BalanceChange> balanceChanges) {
    balanceChanges.forEach(change -> addBalanceChange(change.getBalance(), change.getValue()));
  }

  public BalanceChange addBalanceChange(Balance balance, BigDecimal value) {
    return addBalanceChange(balance, value, Date.valueOf(LocalDate.now()));
  }

  public BalanceChange addBalanceChange(Balance balance, BigDecimal value, @Nullable Date performed) {
    assertNonNulls(balance, value);
    assert value.compareTo(BigDecimal.ZERO) != 0 : this.toString();
    BalanceChange balanceChange = new BalanceChange(UUID.randomUUID(), this, balance, value, performed, balanceChanges.size());
    balanceChanges.add(balanceChange);
    if (value.signum() ==  -1) {
      fromAccId = balance.getId();
      amount = value.abs();
    } else if (value.signum() ==  1) {
      toAccId = balance.getId();
      toAmount = value;
    }
    return balanceChange;
  }

  void changeBalances(boolean revert) {
    BigDecimal factor = revert ? BigDecimal.ONE.negate() : BigDecimal.ONE;
    balanceChanges.forEach(change -> {
      change.getBalance().changeValue(change.getValue().multiply(factor), this);
    });
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MoneyOper moneyOper = (MoneyOper) o;
    return Objects.equals(id, moneyOper.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  boolean essentialEquals(MoneyOper other) {
    assert this.equals(other);
    return balanceChangesEssentialEquals(other);
  }

  boolean balanceChangesEssentialEquals(MoneyOper other) {
//    if (!balanceChanges.equals(other.getBalanceChanges())) {
//      return false;
//    }
    return balanceChanges.stream().allMatch(change -> other.getBalanceChanges()
        .stream()
        .anyMatch(ch -> ch.equals(change)));
  }

  @Override
  public String toString() {
    return "MoneyOper{" +
        "id=" + id +
        ", balanceSheet=" + balanceSheet +
        ", status=" + status +
        ", performed=" + performed +
        ", balanceChanges=" + balanceChanges +
        ", created=" + created +
        '}';
  }
}
