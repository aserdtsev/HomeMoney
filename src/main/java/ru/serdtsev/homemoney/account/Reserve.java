package ru.serdtsev.homemoney.account;

import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.dao.MoneyTrnsDao;
import ru.serdtsev.homemoney.utils.Utils;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.sql.Date;

@Entity
@Table(name= "reserves")
@DiscriminatorValue("reserve")
public class Reserve extends Balance {
  private BigDecimal target;

  @SuppressWarnings({"unused", "WeakerAccess"})
  protected Reserve() {
  }

  public Reserve(BalanceSheet balanceSheet, String name, Date created, Boolean isArc, String currencyCode,
      BigDecimal value, BigDecimal target) {
    super(balanceSheet, AccountType.reserve, name, created, isArc, currencyCode, value, null);
    this.target = target;
  }

  public void init() {
    super.init(null);
    target = Utils.nvl(target, BigDecimal.ZERO);
  }

  public void merge(Reserve reserve, ReserveRepository reserveRepo, MoneyTrnsDao moneyTrnsDao) {
    super.merge(reserve, reserveRepo, moneyTrnsDao);
    setTarget(reserve.getTarget());
  }

  public BigDecimal getTarget() {
    return target;
  }

  public void setTarget(BigDecimal target) {
    this.target = target;
  }
}
