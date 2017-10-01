package ru.serdtsev.homemoney.dao;

import com.google.common.base.Strings;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.serdtsev.homemoney.account.Account;
import ru.serdtsev.homemoney.account.AccountRepository;
import ru.serdtsev.homemoney.account.AccountType;
import ru.serdtsev.homemoney.dto.MoneyTrnTempl;
import ru.serdtsev.homemoney.moneyoper.BalanceChange;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.serdtsev.homemoney.utils.Utils.assertNonNulls;
import static ru.serdtsev.homemoney.utils.Utils.nvl;

@Component
public class MoneyTrnsDao {
  private static final String baseMoneyTrnTemplSelect =
      "select te.id, 'active' as status, te.template_id as sampleId, te.id lastMoneyTrnId, " +
          "    te.next_date as nextDate, te.period, " +
          "    te.from_acc_id as fromAccId, fa.name as fromAccName," +
          "    te.to_acc_id as toAccId, ta.name as toAccName," +
          "    case when fa.type = 'income' then 'income' when ta.type = 'expense' then 'expense' else 'transfer' end as type, " +
          "    te.amount, coalesce(coalesce(fb.currency_code, tb.currency_code), 'RUB') as currencyCode," +
          "    coalesce(te.to_amount, te.amount), coalesce(coalesce(tb.currency_code, fb.currency_code), 'RUB') as toCurrencyCode," +
          "    te.comment " +
          "  from money_trns te, " +
          "    accounts fa " +
          "      left join balances fb on fb.id = fa.id, " +
          "    accounts ta" +
          "      left join balances tb on tb.id = ta.id " +
          "  where te.balance_sheet_id = ? and te.is_template = true" +
          "    and fa.id = te.from_acc_id " +
          "    and ta.id = te.to_acc_id ";

  private AccountRepository accountRepo;
  private LabelsDao labelsDao;

  @Autowired
  public MoneyTrnsDao(AccountRepository accountRepo,
      LabelsDao labelsDao) {
    this.accountRepo = accountRepo;
    this.labelsDao = labelsDao;
  }

  private void createBalanceChange(Connection conn, UUID operId, UUID balanceId, BigDecimal value, @Nullable Date performed,
      int index) throws SQLException {
    assertNonNulls(conn, operId, balanceId, value);
    new QueryRunner().update(conn, "" +
            "insert into balance_changes (id, oper_id, balance_id, value, made, index) " +
            "  values (?, ?, ?, ?, ?, ?)",
        UUID.randomUUID(), operId, balanceId, value, performed, index);
  }

  private static void updateBalanceChange(Connection conn, UUID id, UUID balanceId, BigDecimal value, @Nullable Date performed, int index) {
    assertNonNulls(id, balanceId, value);
    try {
      new QueryRunner().update(conn, "" +
          "update balance_changes set balance_id = ?, value = ?, made = ?, index = ? where id = ?",
          balanceId, value, performed, index, id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private static List<BalanceChange> getBalanceChanges(Connection conn, UUID operId) throws SQLException {
    assertNonNulls(conn, operId);
    return new QueryRunner().query(conn, "" +
        "select id, oper_id as operId, balance_id as balanceId, value, made, index " +
            "from balance_changes " +
            "where oper_id = ? " +
            "order by index",
        new BeanListHandler<>(BalanceChange.class), operId);
  }

  List<MoneyTrnTempl> getMoneyTrnTempls(UUID bsId, String search) {
    try (Connection conn = MainDao.getConnection()) {
      return getMoneyTrnTempls(conn, bsId, search);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public List<MoneyTrnTempl> getMoneyTrnTempls(Connection conn, UUID bsId, String search) throws SQLException {
    StringBuilder sql = new StringBuilder(baseMoneyTrnTemplSelect);
    List<Object> params = new ArrayList<>();
    params.add(bsId);
    if (!Strings.isNullOrEmpty(search)) {
      final String condition = " and (te.comment ilike ? or fa.name ilike ? or ta.name ilike ? " +
          " or exists (select null from labels2objs l2o, labels l where l2o.obj_id = te.id and l.id = l2o.label_id and l.name ilike ?)) ";
      sql.append(condition);
      long paramNum = condition.chars().filter(ch -> ch == '?').count();
      IntStream.range(0, ((int) paramNum)).forEach(i -> params.add("%" + search + "%"));
    }
    sql.append(" order by nextDate desc ");
    return new QueryRunner().query(conn, sql.toString(),
        new BeanListHandler<>(MoneyTrnTempl.class, new BasicRowProcessor(new MoneyTrnProcessor())), params.toArray()).stream()
        .peek(templ -> {
          try {
            templ.setBalanceChanges(getBalanceChanges(conn, templ.getId()));
            templ.setLabels(labelsDao.getLabelNames(conn, templ.getId()));
          } catch (SQLException e) {
            throw new HmSqlException(e);
          }
        })
        .collect(Collectors.toList());
  }

  private MoneyTrnTempl getMoneyTrnTempl(Connection conn, UUID bsId, UUID id) throws SQLException {
    MoneyTrnTempl templ;
      String sql = baseMoneyTrnTemplSelect + " and te.id = ? ";
      templ = new QueryRunner().query(conn, sql,
          new BeanHandler<>(MoneyTrnTempl.class, new BasicRowProcessor(new MoneyTrnProcessor())), bsId, id);
      templ.setBalanceChanges(getBalanceChanges(conn, templ.getId()));
      templ.setLabels(labelsDao.getLabelNames(conn, id));
    return templ;
  }

  public void updateMoneyTrnTempl(UUID bsId, MoneyTrnTempl templ) {
    try (Connection conn = MainDao.getConnection()) {
      updateMoneyTrnTempl(conn, bsId, templ);
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private void updateMoneyTrnTempl(Connection conn, UUID bsId, MoneyTrnTempl templ) throws SQLException {
    int rowCount = new QueryRunner().update(conn,
        "update money_trn_templs set " +
            "  sample_id = ?, " +
            "  last_money_trn_id = ?, " +
            "  next_date = ?, " +
            "  amount = ?, " +
            "  to_amount = ?, " +
            "  from_acc_id = ?, " +
            "  to_acc_id = ?, " +
            "  comment = ?, " +
            "  period = ? " +
            " where bs_id = ? and id = ?",
        templ.getSampleId(),
        templ.getLastMoneyTrnId(),
        templ.getNextDate(),
        templ.getAmount(),
        templ.getToAmount(),
        templ.getFromAccId(),
        templ.getToAccId(),
        templ.getComment(),
        templ.getPeriod().name(),
        bsId,
        templ.getId());
    if (rowCount == 0) {
      throw new IllegalArgumentException(String.format("Обновляемый шаблон %s не найден.",
          templ.getId()));
    }

    List<BalanceChange> balanceChanges = MoneyTrnsDao.getBalanceChanges(conn, templ.getId());
    balanceChanges.stream()
        .filter(balanceChange -> balanceChange.getValue().compareTo(BigDecimal.ZERO) < 0)
        .findFirst()
        .ifPresent(balanceChange ->
            MoneyTrnsDao.updateBalanceChange(conn, balanceChange.getId(), templ.getFromAccId(), templ.getAmount().negate(),
                null, balanceChange.getIndex()));
    balanceChanges.stream()
        .filter(balanceChange -> balanceChange.getValue().compareTo(BigDecimal.ZERO) > 0)
        .findFirst()
        .ifPresent(balanceChange -> {
          BigDecimal toAmount = templ.getToAmount() != null ? templ.getToAmount() : templ.getAmount();
          MoneyTrnsDao.updateBalanceChange(conn, balanceChange.getId(), templ.getToAccId(), toAmount,
              null, balanceChange.getIndex());
        });

    labelsDao.saveLabels(conn, bsId, templ.getId(), "template", templ.getLabels());
  }

  public void createMoneyTrnTempl(UUID bsId, MoneyTrnTempl templ) {
    try (Connection conn = MainDao.getConnection()) {
      List<String> labels = templ.getLabels();

      new QueryRunner().update(conn,
          "insert into money_trn_templs(id, status, bs_id, sample_id, last_money_trn_id, " +
              "next_date, amount, from_acc_id, to_acc_id, comment, period) " +
              "  values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          templ.getId(), templ.getStatus().name(), bsId, templ.getSampleId(), templ.getLastMoneyTrnId(),
          templ.getNextDate(), templ.getAmount(), templ.getFromAccId(), templ.getToAccId(), templ.getComment(),
          templ.getPeriod().name());

      templ = getMoneyTrnTempl(conn, bsId, templ.getId());

      if (templ.getType().equals("expense") || templ.getType().equals("transfer")) {
        createBalanceChange(conn, templ.getId(), templ.getFromAccId(), templ.getAmount().negate(), null, 0);
      }

      if (templ.getType().equals("income") || templ.getType().equals("transfer")) {
        BigDecimal toAmount = nvl(templ.getToAmount(), templ.getAmount());
        createBalanceChange(conn, templ.getId(), templ.getToAccId(), toAmount, null, 1);
      }

      Account fromAcc = accountRepo.findOne(templ.getFromAccId());
      if (fromAcc.getType() == AccountType.income) {
        labels.add(fromAcc.getName());
      }

      Account toAcc = accountRepo.findOne(templ.getToAccId());
      if (toAcc.getType() == AccountType.expense) {
        labels.add(toAcc.getName());
      }

      labels.remove("<Без категории>");

      labelsDao.saveLabels(conn, bsId, templ.getId(), "template", templ.getLabels());
      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  public static Boolean isTrnTemplExists(UUID id) {
    try (Connection conn = MainDao.getConnection()) {
      return isTrnTemplExists(conn, id);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  static Boolean isTrnTemplExists(Connection conn, UUID id) throws SQLException {
    long trnCount = new QueryRunner().query(conn,
        "select count(*) from money_trn_templs where from_acc_id = ? or to_acc_id = ?",
        new ScalarHandler<Long>(), id, id);
    return trnCount > 0;
  }

}
