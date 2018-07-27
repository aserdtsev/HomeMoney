package ru.serdtsev.homemoney.moneyoper.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data @AllArgsConstructor
public class MoneyOperItemDto {
  private UUID id;
  private UUID balanceId;
  private String balanceName;
  private BigDecimal value;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate performedAt;
  private int index;
}
