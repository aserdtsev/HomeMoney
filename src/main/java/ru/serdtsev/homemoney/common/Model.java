package ru.serdtsev.homemoney.common;

import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import java.util.Objects;
import java.util.UUID;

public abstract class Model {
  protected final UUID id;

  public Model(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BalanceSheet that = (BalanceSheet) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
