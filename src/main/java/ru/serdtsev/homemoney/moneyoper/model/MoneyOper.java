package ru.serdtsev.homemoney.moneyoper.model;

import org.apache.logging.log4j.util.Strings;
import ru.serdtsev.homemoney.account.Account;
import ru.serdtsev.homemoney.account.AccountRepository;
import ru.serdtsev.homemoney.account.AccountType;
import ru.serdtsev.homemoney.account.Balance;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.AssertTrue;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus.*;
import static ru.serdtsev.homemoney.utils.Utils.assertNonNulls;

@Entity
@Table(name = "money_oper")
public class MoneyOper implements Serializable {
  @Id
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "balance_sheet_id")
  @Nonnull
  private BalanceSheet balanceSheet;

  @Enumerated(EnumType.STRING)
  @Nonnull
  private MoneyOperStatus status;

  @Column(name = "trn_date")
  @Nonnull
  private LocalDate performed;

  @Column(name = "date_num")
  private Integer dateNum;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "oper_id")
  @Nonnull
  private List<MoneyOperItem> items;

  @ManyToMany
  @JoinTable(name = "labels2objs",
      joinColumns = @JoinColumn(name = "obj_id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)),
      inverseJoinColumns = @JoinColumn(name = "label_id"))
  private Set<Label> labels;

  private String comment;

  @Enumerated(EnumType.STRING)
  private Period period;

  @Column(name = "created_ts")
  @Nonnull
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

  public MoneyOper(UUID id, BalanceSheet balanceSheet, MoneyOperStatus status, LocalDate performed, Integer dateNum,
      Collection<Label> labels, String comment, Period period) {
    this.id = id;
    this.balanceSheet = balanceSheet;
    this.status = status;
    this.performed = performed;
    this.dateNum = dateNum;
    this.items = new ArrayList<>();
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

  public LocalDate getPerformed() {
    return performed;
  }

  public void setPerformed(LocalDate performed) {
    this.performed = performed;
  }

  public List<MoneyOperItem> getItems() {
    return items;
  }

  public void setItems(List<MoneyOperItem> items) {
    this.items = items;
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

  @Deprecated
  public UUID getFromAccId() {
    return fromAccId;
  }

  public void setToAccId(UUID toAccId) {
    this.toAccId = toAccId;
  }

  @Deprecated
  public UUID getToAccId() {
    return toAccId;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  @Deprecated
  public BigDecimal getAmount() {
    return items.stream()
        .map(MoneyOperItem::getValue)
        .filter(value -> value.signum() < 0)
        .map(BigDecimal::abs)
        .findFirst()
        .orElse(amount);
  }

  @Deprecated
  public String getCurrencyCode() {
    return items.stream()
        .sorted(Comparator.comparingInt(item -> item.getValue().signum()))
        .map(item -> item.getBalance().getCurrencyCode())
        .findFirst()
        .orElse(getBalanceSheet().getCurrencyCode());
  }

  public void setToAmount(BigDecimal toAmount) {
    this.toAmount = toAmount;
  }

  @Deprecated
  public BigDecimal getToAmount() {
    return items.stream()
        .map(MoneyOperItem::getValue)
        .filter(value -> value.signum() > 0)
        .map(BigDecimal::abs)
        .findFirst()
        .orElse(toAmount);
  }

  public String getToCurrencyCode() {
    return items.stream()
        .sorted(Comparator.comparingInt(item -> item.getValue().signum() * -1))
        .map(item -> item.getBalance().getCurrencyCode())
        .findFirst()
        .orElse(getBalanceSheet().getCurrencyCode());
  }

  public UUID getRecurrenceId() {
    return recurrenceId;
  }

  public void setRecurrenceId(UUID recurrenceId) {
    this.recurrenceId = recurrenceId;
  }

  public MoneyOperType getType() {
    if (items.stream().findFirst().orElseThrow(IllegalStateException::new).getBalance().getType() == AccountType.reserve) {
      return MoneyOperType.transfer;
    }
    Optional<Integer> valueSignumSumOpt = items.stream()
        .map(item -> item.getValue().signum())
        .reduce((a, s) -> a += s);
    int valueSignumSum = valueSignumSumOpt.orElseThrow(() ->
        new IllegalStateException("items is empty: " + id));
    if (valueSignumSum > 0) {
      return MoneyOperType.income;
    } else if (valueSignumSum < 0) {
      return MoneyOperType.expense;
    }
    return MoneyOperType.transfer;
  }

  @Deprecated
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
    assert !performed.isAfter(LocalDate.now());
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

  public void addItems(Collection<MoneyOperItem> items) {
    items.forEach(item -> addItem(item.getBalance(), item.getValue()));
  }

  public MoneyOperItem addItem(Balance balance, BigDecimal value) {
    return addItem(balance, value, LocalDate.now());
  }

  public MoneyOperItem addItem(Balance balance, BigDecimal value, @Nullable LocalDate performed) {
    assertNonNulls(balance, value);
    assert value.compareTo(BigDecimal.ZERO) != 0 : this.toString();
    MoneyOperItem item = new MoneyOperItem(UUID.randomUUID(), this, balance, value, performed, items.size());
    items.add(item);
    if (value.signum() ==  -1) {
      fromAccId = balance.getId();
      amount = value.abs();
    } else if (value.signum() ==  1) {
      toAccId = balance.getId();
      toAmount = value;
    }
    return item;
  }

  public void changeBalances(boolean revert) {
    BigDecimal factor = revert ? BigDecimal.ONE.negate() : BigDecimal.ONE;
    items.forEach(item -> item.getBalance().changeValue(item.getValue().multiply(factor), this));
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

  public boolean essentialEquals(MoneyOper other) {
    assert this.equals(other);
    return itemsEssentialEquals(other);
  }

  boolean itemsEssentialEquals(MoneyOper other) {
    return items.stream().allMatch(item ->
        other.getItems().stream().anyMatch(i -> i.equals(item)));
  }

  @Override
  public String toString() {
    return "MoneyOper{" +
        "id=" + id +
        ", balanceSheet=" + balanceSheet +
        ", status=" + status +
        ", performed=" + performed +
        ", items=" + items +
        ", created=" + created +
        '}';
  }

  @AssertTrue(message = "Fields amount and toAmount is different.")
  public boolean isAmountsValid() {
    return !Objects.equals(getCurrencyCode(), getToCurrencyCode()) || amount.compareTo(toAmount) == 0;
  }

  @AssertTrue(message = "Field recurrenceId of template is null.")
  public boolean isRecurrenceIdNotNullForTemplate() {
    return status != MoneyOperStatus.template || Objects.nonNull(recurrenceId);
  }
}
