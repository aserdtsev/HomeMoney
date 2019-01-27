package ru.serdtsev.homemoney.moneyoper.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.serdtsev.homemoney.common.HmException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class MoneyOperDto {
  private UUID id;
  private MoneyOperStatus status;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate operDate;
  private Integer dateNum;
  private List<MoneyOperItemDto> items;
  private UUID fromAccId;
  private UUID toAccId;
  private UUID parentId;
  private BigDecimal amount;
  private String currencyCode;
  private BigDecimal toAmount;
  private String toCurrencyCode;
  private String comment;
  private Timestamp createdTs;
  private Period period;
  private List<String> labels;
  private UUID recurrenceId;
  private String fromAccName;
  private String toAccName;
  private String type;

  public MoneyOperDto(UUID id, MoneyOperStatus status, LocalDate operDate,
      UUID fromAccId, UUID toAccId, BigDecimal amount, String currencyCode, BigDecimal toAmount, String toCurrencyCode,
      Period period, String comment, List<String> labels, Integer dateNum, UUID parentId, UUID recurrenceId, Timestamp createdTs) {
    if (amount.compareTo(BigDecimal.ZERO) == 0) {
      throw new HmException(HmException.Code.WrongAmount);
    }
    this.id = id;
    this.status = status;
    this.operDate = operDate;
    this.dateNum = dateNum;
    this.fromAccId = fromAccId;
    this.toAccId = toAccId;
    this.parentId = parentId;
    this.amount = amount;
    this.currencyCode = currencyCode;
    this.toAmount = toAmount;
    this.toCurrencyCode = toCurrencyCode;
    this.comment = comment;
    this.createdTs = createdTs != null ? createdTs : Timestamp.valueOf(LocalDateTime.now());
    this.period = period;
    this.labels = labels;
    this.recurrenceId = recurrenceId;

    this.items = new ArrayList<>();
  }

  public String getCurrencySymbol() {
    return Currency.getInstance(currencyCode).getSymbol();
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public String getToCurrencySymbol() {
    return Currency.getInstance(toCurrencyCode).getSymbol();
  }

  public Boolean isMonoCurrencies() {
    return currencyCode.equals(toCurrencyCode);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MoneyOperDto moneyOperDto = (MoneyOperDto) o;

    return getId() != null ? getId().equals(moneyOperDto.getId()) : moneyOperDto.getId() == null;
  }

  @Override
  public int hashCode() {
    return getId() != null ? getId().hashCode() : 0;
  }

  @Override
  public String toString() {
    return "MoneyOperDto{" +
        "id=" + id +
        ", status=" + status +
        ", operDate=" + operDate +
        ", items=" + items +
        ", dateNum=" + dateNum +
        ", fromAccId=" + fromAccId +
        ", toAccId=" + toAccId +
        ", parentId=" + parentId +
        ", amount=" + amount +
        ", currencyCode='" + currencyCode + '\'' +
        ", toAmount=" + toAmount +
        ", toCurrencyCode='" + toCurrencyCode + '\'' +
        ", comment='" + comment + '\'' +
        ", createdTs=" + createdTs +
        ", period=" + period +
        ", labels=" + labels +
        ", recurrenceId=" + recurrenceId +
        ", fromAccName='" + fromAccName + '\'' +
        ", toAccName='" + toAccName + '\'' +
        ", type='" + type + '\'' +
        '}';
  }
}
