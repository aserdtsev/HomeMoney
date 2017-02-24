package ru.serdtsev.homemoney.balancesheet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import ru.serdtsev.homemoney.common.Model;
import ru.serdtsev.homemoney.dao.AccountsDao;
import ru.serdtsev.homemoney.dao.HmSqlException;
import ru.serdtsev.homemoney.dao.MainDao;
import ru.serdtsev.homemoney.dto.Account;
import ru.serdtsev.homemoney.dto.BalanceSheetDto;
import ru.serdtsev.homemoney.moneyoper.MoneyOper;
import ru.serdtsev.homemoney.moneyoper.MoneyOperStatus;
import ru.serdtsev.homemoney.utils.Utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class BalanceSheet extends Model {
  private Timestamp created;
  private String currencyCode;

  private UUID svcRsvId;
  private UUID uncatCostsId;
  private UUID uncatIncomeId;

  private static final String fieldNames = "id, currency_code, created_ts, svc_rsv_id, uncat_costs_id, uncat_income_id";
  private static final String baseSelect = "select " + fieldNames + " from balance_sheets";

  private BalanceSheet(UUID id, Timestamp created, String currencyCode) {
    this(id, created, currencyCode, null, null, null);
  }

  private BalanceSheet(UUID id, Timestamp created, String currencyCode, @Nullable UUID svcRsvId,
      @Nullable UUID uncatCostsId, @Nullable UUID uncatIncomeId) {
    super(id);
    this.created = created;
    this.currencyCode = currencyCode;
    this.svcRsvId = svcRsvId;
    this.uncatCostsId = uncatCostsId;
    this.uncatIncomeId = uncatIncomeId;
  }

  public static BalanceSheet newInstance() {
    return new BalanceSheet(UUID.randomUUID(), Timestamp.valueOf(LocalDateTime.now()), "RUB")
        .init()
        .save();
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
      save();

      svcRsvId = UUID.randomUUID();
      AccountsDao.createAccount(conn, id, new Account(svcRsvId, Account.Type.service, "Service reserve"));

      uncatCostsId = UUID.randomUUID();
      AccountsDao.createAccount(conn, id, new Account(uncatCostsId, Account.Type.expense, "<Без категории>"));

      uncatIncomeId = UUID.randomUUID();
      AccountsDao.createAccount(conn, id, new Account(uncatIncomeId, Account.Type.income, "<Без категории>"));

      save();
      return this;
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public BalanceSheet save() {
    return isNew() ? insert() : update();
  }

  private BalanceSheet insert() {
    assert isNew();
    Utils.assertNonNulls(id, currencyCode, created);
    MainDao.jdbcTemplate().update("" +
            "insert into balance_sheets(" + fieldNames + ") values (?, ?, ?, ?, ?, ?)",
        id, currencyCode, created, svcRsvId, uncatCostsId, uncatIncomeId);
    return this;
  }

  private BalanceSheet update() {
    assert !isNew();
    Utils.assertNonNulls(id, currencyCode, created, svcRsvId, uncatCostsId, uncatIncomeId);
    MainDao.jdbcTemplate().update("" +
            "update balance_sheets set svc_rsv_id = ?, uncat_costs_id = ?, uncat_income_id = ? where id = ?",
        svcRsvId, uncatCostsId, uncatIncomeId, id);
    return this;
  }

  @SuppressWarnings("unused")
  public void delete() {
    MainDao.jdbcTemplate().update("delete from balance_sheets where id = ?", id);
  }

  public static List<BalanceSheet> getAllInstances() {
    return MainDao.jdbcTemplate().query(baseSelect + " order by created_ts desc", getBalanceSheetRowMapper());
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

  boolean isNew() {
    int count = MainDao.jdbcTemplate().queryForObject("select count(*) from balance_sheets where id = ?",
        Integer.class, id);
    return count == 0;
  }

  @SuppressWarnings("unused")
  public Stream<MoneyOper> getMoneyOpers(Integer limit, @Nullable Integer offset, @Nullable String search,
      MoneyOperStatus... statuses) {
    return null;
  }
}
