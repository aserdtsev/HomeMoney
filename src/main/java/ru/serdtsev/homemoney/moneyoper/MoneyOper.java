package ru.serdtsev.homemoney.moneyoper;

import ru.serdtsev.homemoney.dto.BalanceChange;
import ru.serdtsev.homemoney.dto.MoneyTrn;

import javax.persistence.Id;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

//@Entity
//@Table(name = "money_trns")
public class MoneyOper {

  @Id
  private UUID id;
  private UUID bsId;
  private MoneyOperStatus status;
  private Date performed;
  private Integer dateNum;
  private List<BalanceChange> balanceChanges;
  private List<Label> labels;
  private String comment;
  private Period period;
  private Timestamp created;
  private UUID recurrenceOperId;

  public MoneyOper(UUID id, UUID bsId, MoneyOperStatus status, Date performed, Integer dateNum,
      List<BalanceChange> balanceChanges, List<Label> labels, String comment, Period period) {
    this.id = id;
    this.bsId = bsId;
    this.status = status;
    this.performed = performed;
    this.dateNum = dateNum;
    this.balanceChanges = balanceChanges;
    this.labels = labels;
    this.comment = comment;
    this.period = period;
  }

  public MoneyOper setRecurrenceOper(MoneyOper recurrenceOper) {
    this.recurrenceOperId = recurrenceOper.getId();
    return this;
  }

  public UUID getId() {
    return id;
  }

  public static MoneyOper fromDto(MoneyTrn dto) {
    return null;
  }

  public MoneyTrn toDto() {
    return null;
  }

}
