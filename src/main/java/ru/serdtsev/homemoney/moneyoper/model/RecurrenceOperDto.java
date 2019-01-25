package ru.serdtsev.homemoney.moneyoper.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class RecurrenceOperDto {
  private UUID id;
  private Status status;
  private UUID sampleId;
  private UUID lastMoneyTrnId;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate nextDate;
  private Period period;
  private List<MoneyOperItemDto> items;
  private UUID fromAccId;
  private UUID toAccId;
  private BigDecimal amount;
  private String comment;
  private List<String> labels;
  private String currencyCode;
  private BigDecimal toAmount;
  private String toCurrencyCode;
  private String fromAccName;
  private String toAccName;
  private String type;

  public RecurrenceOperDto(UUID id, UUID sampleId, UUID lastMoneyTrnId, LocalDate nextDate, Period period,
      UUID fromAccId, UUID toAccId, BigDecimal amount, BigDecimal toAmount, String comment, List<String> labels,
      String currencyCode, String toCurrencyCode, String fromAccName, String toAccName, String type) {
    this.id = id;
    this.sampleId = sampleId;
    this.lastMoneyTrnId = lastMoneyTrnId;
    this.nextDate = nextDate;
    this.period = period;
    this.fromAccId = fromAccId;
    this.toAccId = toAccId;
    this.amount = amount;
    this.toAmount = toAmount;
    this.comment = comment;
    this.labels = labels;
    this.currencyCode = currencyCode;
    this.toCurrencyCode = toCurrencyCode;
    this.fromAccName = fromAccName;
    this.toAccName = toAccName;
    this.type = type;
    this.items = new ArrayList<>();
  }

  public enum Status {
    active,
    deleted
  }
}
