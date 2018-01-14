package ru.serdtsev.homemoney.moneyoper.model;

import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "recurrence_oper")
public class RecurrenceOper {
  @Id
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "bs_id")
  private BalanceSheet balanceSheet;

  @OneToOne
  @JoinColumn(name = "template_id")
  private MoneyOper template;

  @Column(name = "next_date")
  private LocalDate nextDate;

  @Column(name = "is_arc")
  private Boolean isArc;

  private RecurrenceOper() {
  }

  public RecurrenceOper(UUID id, BalanceSheet balanceSheet, MoneyOper template, LocalDate nextDate) {
    this.id = id;
    this.balanceSheet = balanceSheet;
    this.template = template;
    this.nextDate = nextDate;
    this.isArc = false;
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

  public MoneyOper getTemplate() {
    return template;
  }

  public void setTemplate(MoneyOper template) {
    this.template = template;
  }

  public LocalDate getNextDate() {
    return nextDate;
  }

  public void setNextDate(LocalDate nextDate) {
    this.nextDate = nextDate;
  }

  public LocalDate skipNextDate() {
    nextDate = calcNextDate(nextDate);
    return nextDate;
  }

  public LocalDate calcNextDate(LocalDate date) {
    LocalDate dateAsLocalDate = date;
    LocalDate nextDate;
    switch (template.getPeriod()) {
      case month:
        nextDate = dateAsLocalDate.plusMonths(1);
        break;
      case quarter:
        nextDate = dateAsLocalDate.plusMonths(3);
        break;
      case year:
        nextDate =  dateAsLocalDate.plusYears(1);
        break;
      default:
        nextDate = date;
    }
    return nextDate;
  }

  public Boolean getArc() {
    return isArc;
  }

  private void setArc(Boolean arc) {
    isArc = arc;
  }

  /**
   * Переводит повторяющуюся операцию в архив.
   */
  public void arc() {
    template.cancel();
    setArc(true);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof RecurrenceOper)) return false;
    RecurrenceOper that = (RecurrenceOper) o;
    return Objects.equals(balanceSheet, that.balanceSheet) &&
        Objects.equals(template, that.template);
  }

  @Override
  public int hashCode() {
    return Objects.hash(balanceSheet, template);
  }

  @Override
  public String toString() {
    return "RecurrenceOper{" +
        "id=" + id +
        ", balanceSheet=" + balanceSheet +
        ", template=" + template +
        ", nextDate=" + nextDate +
        '}';
  }
}