package ru.serdtsev.homemoney.dto;

import javax.xml.bind.annotation.XmlElement;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public class Balance extends Account {
  private UUID reserveId;
  private BigDecimal value;
  private BigDecimal creditLimit;
  private BigDecimal minValue;
  private Long num;

  public Balance() {
  }

  public Balance(UUID id, Type type, String name, BigDecimal value, UUID reserveId,
      BigDecimal creditLimit, BigDecimal minValue) {
    super(id, type, name);
    setValue(value);
    setReserveId(reserveId);
    setCreditLimit(creditLimit);
    setMinValue(minValue);
  }

  public BigDecimal getValue() {
    return Optional.ofNullable(value).orElse(BigDecimal.ZERO);
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  public UUID getReserveId() {
    return reserveId;
  }

  public void setReserveId(UUID reserveId) {
    this.reserveId = reserveId;
  }

  public BigDecimal getCreditLimit() {
    return Optional.ofNullable(creditLimit).orElse(BigDecimal.ZERO);
  }

  public void setCreditLimit(BigDecimal creditLimit) {
    this.creditLimit = creditLimit;
  }

  public BigDecimal getMinValue() {
    return Optional.ofNullable(minValue).orElse(BigDecimal.ZERO);
  }

  public void setMinValue(BigDecimal minValue) {
    this.minValue = minValue;
  }

  public Long getNum() {
    return num;
  }

  public void setNum(Long num) {
    this.num = num;
  }

  @XmlElement(name="freeFunds")
  public BigDecimal getFreeFunds() {
    return getValue().add(getCreditLimit().subtract(getMinValue()));
  }

  @XmlElement(name="freeFunds")
  public void setFreeFunds(BigDecimal availableBalance) {
    // Поле вычисляемое. Метод нужен для сериализации класса из JSON.
  }
}
