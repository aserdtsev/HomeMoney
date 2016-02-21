package ru.serdtsev.homemoney.dto;

import javax.xml.bind.annotation.XmlElement;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public class Balance extends Account {
  private Optional<UUID> reserveId;
  private Optional<BigDecimal> value;
  private Optional<BigDecimal> creditLimit;
  private Optional<BigDecimal> minValue;
  private Long num;

  @SuppressWarnings("unused")
  public Balance() {
    this.value = Optional.empty();
    this.reserveId = Optional.empty();
    this.creditLimit = Optional.empty();
    this.minValue = Optional.empty();
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
    return value.orElse(BigDecimal.ZERO);
  }

  public void setValue(BigDecimal value) {
    this.value = Optional.ofNullable(value);
  }

  @SuppressWarnings("unused")
  public UUID getReserveId() {
    return reserveId.orElse(null);
  }

  @SuppressWarnings("unused")
  public void setReserveId(UUID reserveId) {
    this.reserveId = Optional.ofNullable(reserveId);
  }

  public BigDecimal getCreditLimit() {
    return creditLimit.orElse(BigDecimal.ZERO);
  }

  public void setCreditLimit(BigDecimal creditLimit) {
    this.creditLimit = Optional.ofNullable(creditLimit);
  }

  public BigDecimal getMinValue() {
    return minValue.orElse(BigDecimal.ZERO);
  }

  public void setMinValue(BigDecimal minValue) {
    this.minValue = Optional.ofNullable(minValue);
  }

  @SuppressWarnings("unused")
  public Long getNum() {
    return num;
  }

  @SuppressWarnings("unused")
  public void setNum(Long num) {
    this.num = num;
  }

  @XmlElement(name="freeFunds")
  @SuppressWarnings("unused")
  public BigDecimal getFreeFunds() {
    return getValue().add(getCreditLimit().subtract(getMinValue()));
  }

  @XmlElement(name="freeFunds")
  @SuppressWarnings("unused")
  public void setFreeFunds(BigDecimal availableBalance) {
    // Поле вычисляемое. Метод нужен для сериализации класса из JSON.
  }
}
