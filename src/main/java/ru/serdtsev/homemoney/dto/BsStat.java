package ru.serdtsev.homemoney.dto;

import javax.xml.bind.annotation.XmlTransient;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public class BsStat {
  private UUID bsId;
  private Date fromDate;
  private Date toDate;
  public BigDecimal incomeAmount = BigDecimal.ZERO;
  public BigDecimal chargesAmount = BigDecimal.ZERO;
  public List<BsDayStat> dayStats;

  @XmlTransient
  public Map<Account.Type, BigDecimal> saldoMap = new HashMap<>();

  public BsStat(UUID bsId, Date fromDate, Date toDate) {
    this.bsId = bsId;
    this.fromDate = fromDate;
    this.toDate = toDate;
  }

  public UUID getBsId() {
    return bsId;
  }

  public BigDecimal getDebitSaldo() {
    return saldoMap.getOrDefault(Account.Type.debit, BigDecimal.ZERO);
  }
  public BigDecimal getCreditSaldo() {
    return saldoMap.getOrDefault(Account.Type.credit, BigDecimal.ZERO);
  }
  public BigDecimal getAssetSaldo() {
    return saldoMap.getOrDefault(Account.Type.asset, BigDecimal.ZERO);
  }
  public BigDecimal getReserveSaldo() {
    return saldoMap.getOrDefault(Account.Type.reserve, BigDecimal.ZERO);
  }
  public BigDecimal getTotalSaldo() {
    return getDebitSaldo().add(getCreditSaldo()).add(getAssetSaldo());
  }
}
