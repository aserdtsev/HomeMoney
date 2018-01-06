package ru.serdtsev.homemoney.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
public class CategoryStat {
  private UUID id;
  private UUID rootId;
  private String name;
  private BigDecimal amount;
}
