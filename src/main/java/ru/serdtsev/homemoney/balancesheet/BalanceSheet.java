package ru.serdtsev.homemoney.balancesheet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import ru.serdtsev.homemoney.dao.AccountsDao;
import ru.serdtsev.homemoney.dao.HmSqlException;
import ru.serdtsev.homemoney.dao.MainDao;
import ru.serdtsev.homemoney.dto.Account;
import ru.serdtsev.homemoney.dto.BalanceSheetDto;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "balance_sheets")
public class BalanceSheet {
  @Id
  private UUID id;

  private Timestamp created;

  @Column(name = "currency_code")
  private String currencyCode;

  @Column(name = "svc_rsv_id")
  private UUID svcRsvId;

  @Column(name = "uncat_costs_id")
  private UUID uncatCostsId;

  @Column(name = "uncat_income_id")
  private UUID uncatIncomeId;

  private static final String fieldNames = "id, currency_code, created_ts, svc_rsv_id, uncat_costs_id, uncat_income_id";
  private static final String baseSelect = "select " + fieldNames + " from balance_sheets";

  @SuppressWarnings({"unused", "WeakerAccess"})
  private BalanceSheet() { }

  private BalanceSheet(UUID id, Timestamp created, String currencyCode) {
    this(id, created, currencyCode, null, null, null);
  }

  private BalanceSheet(UUID id, Timestamp created, String currencyCode, @Nullable UUID svcRsvId,
      @Nullable UUID uncatCostsId, @Nullable UUID uncatIncomeId) {
    this.id = id;
    this.created = created;
    this.currencyCode = currencyCode;
    this.svcRsvId = svcRsvId;
    this.uncatCostsId = uncatCostsId;
    this.uncatIncomeId = uncatIncomeId;
  }

  public UUID getId() {
    return id;
  }

  public static BalanceSheet newInstance() {
    return new BalanceSheet(UUID.randomUUID(), Timestamp.valueOf(LocalDateTime.now()), "RUB")
        .init();
  }

  public static BalanceSheet getInstance(UUID id) {
    return MainDao.jdbcTemplate().queryForObject(baseSelect + " where id = ?", new Object[] {id},
        getBalanceSheetRowMapper());
  }

  public static BalanceSheet fromDto(BalanceSheetDto dto) {
    return new BalanceSheet(dto.getId(), dto.getCreatedTs(), dto.getCurrencyCode());
  }

  public BalanceSheetDto toDto() {
    return new BalanceSheetDto(id, created, svcRsvId, uncatCostsId, uncatIncomeId, currencyCode);
  }

  public BalanceSheet init() {
    try (Connection conn = MainDao.jdbcTemplate().getDataSource().getConnection()) {
      svcRsvId = UUID.randomUUID();
      AccountsDao.createAccount(conn, id, new Account(svcRsvId, Account.Type.service, "Service reserve"));

      uncatCostsId = UUID.randomUUID();
      AccountsDao.createAccount(conn, id, new Account(uncatCostsId, Account.Type.expense, "<Без категории>"));

      uncatIncomeId = UUID.randomUUID();
      AccountsDao.createAccount(conn, id, new Account(uncatIncomeId, Account.Type.income, "<Без категории>"));

      return this;
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }


  @NotNull
  private static RowMapper<BalanceSheet> getBalanceSheetRowMapper() {
    return (rs, rowNum) -> new BalanceSheet(
        rs.getObject("id", UUID.class),
        rs.getTimestamp("created_ts"), rs.getString("currency_code"),
        rs.getObject("svc_rsv_id", UUID.class),
        rs.getObject("uncat_costs_id", UUID.class),
        rs.getObject("uncat_income_id", UUID.class));
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public UUID getSvcRsvId() {
    return svcRsvId;
  }

  public UUID getUncatCostsId() {
    return uncatCostsId;
  }

  public UUID getUncatIncomeId() {
    return uncatIncomeId;
  }

}
