package ru.serdtsev.homemoney.balancesheet;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.serdtsev.homemoney.account.Account;
import ru.serdtsev.homemoney.account.AccountRepository;
import ru.serdtsev.homemoney.account.AccountType;
import ru.serdtsev.homemoney.dto.*;
import ru.serdtsev.homemoney.moneyoper.RecurrenceOperRepo;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus;
import ru.serdtsev.homemoney.moneyoper.model.RecurrenceOper;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StatService {
  private final BalanceSheetRepository balanceSheetRepo;
  private final RecurrenceOperRepo recurrenceOperRepo;
  private final AccountRepository accountRepo;
  private final DataSource dataSource;
  private final JdbcTemplate jdbcTemplate;

  public BsStat getBsStat(UUID bsId, Long interval) {
    LocalDate today = LocalDate.now();
    LocalDate toDate = today;
    LocalDate fromDate = today.minusDays(interval);

    BsStat bsStat = new BsStat(bsId, fromDate, toDate);
    calcCurrentSaldo(bsStat);

    TreeMap<LocalDate, BsDayStat> map = new TreeMap<>();
    List<Turnover> realTurnovers = getRealTurnovers(bsId, MoneyOperStatus.done, fromDate, toDate);
    fillBsDayStatMap(map, realTurnovers);
    calcPastSaldoNTurnovers(bsStat, map);

    LocalDate trendFromDate = today.plusDays(1).minusMonths(1);
    LocalDate trendToDate = trendFromDate.plusDays(interval - 1);
    TreeMap<LocalDate, BsDayStat> trendMap = new TreeMap<>();
    fillBsDayStatMap(trendMap, getTrendTurnovers(bsId, trendFromDate, trendToDate));
    fillBsDayStatMap(trendMap, getRealTurnovers(bsId, MoneyOperStatus.pending, LocalDate.MIN, today.plusDays(interval)));
    fillBsDayStatMap(trendMap, getRecurrenceTurnovers(bsId, today.plusDays(interval)));
    calcTrendSaldoNTurnovers(bsStat, trendMap);

    map.putAll(trendMap);
    bsStat.setDayStats(new ArrayList<>(map.values()));
    bsStat.setCategories(getCategoies(bsId, fromDate, toDate));

    return bsStat;
  }

  /**
   * Вычисляет текущие балансы счетов и резервов.
   */
  private void calcCurrentSaldo(BsStat bsStat) {
    List<AggrAccSaldo> aggrAccSaldo = jdbcTemplate.query(
        "select type, sum(saldo) as saldo from v_crnt_saldo_by_base_cry where bs_id = ? group by type",
        (rs, rowNum) -> {
          AccountType type = AccountType.valueOf(rs.getString("type"));
          BigDecimal saldo = rs.getBigDecimal("saldo");
          return new AggrAccSaldo(type, saldo);
        },
        bsStat.getBsId());

    aggrAccSaldo.forEach(saldo -> bsStat.getSaldoMap().put(saldo.getType(), saldo.getSaldo()));
  }

  private void calcPastSaldoNTurnovers(BsStat bsStat, Map<LocalDate, BsDayStat> map) {
    Map<AccountType, BigDecimal> saldoMap = new HashMap<>(AccountType.values().length);
    bsStat.getSaldoMap().forEach((type, value) -> saldoMap.put(type, value.plus()));
    List<BsDayStat> dayStats = new ArrayList<>(map.values());
    dayStats.sort((e1, e2) -> (e1.getLocalDate().isAfter(e2.getLocalDate())) ? -1 : 1);
    dayStats.forEach(dayStat -> {
      Arrays.asList(AccountType.values()).forEach(type -> {
        dayStat.setSaldo(type, saldoMap.getOrDefault(type, BigDecimal.ZERO));
        saldoMap.put(type, (saldoMap).getOrDefault(type, BigDecimal.ZERO).subtract(dayStat.getDelta(type)));
      });
      bsStat.setIncomeAmount(bsStat.getIncomeAmount().add(dayStat.getIncomeAmount()));
      bsStat.setChargesAmount(bsStat.getChargesAmount().add(dayStat.getChargeAmount()));
    });
  }

  private void calcTrendSaldoNTurnovers(BsStat bsStat, Map<LocalDate, BsDayStat> trendMap) {
    List<BsDayStat> dayStats = new ArrayList<>(trendMap.values());
    Map<AccountType, BigDecimal> saldoMap = new HashMap<>(AccountType.values().length);
    bsStat.getSaldoMap().forEach((type, value) -> saldoMap.put(type, value.plus()));
    dayStats.forEach(dayStat ->
        Arrays.asList(AccountType.values()).forEach(type -> {
          BigDecimal saldo = saldoMap.getOrDefault(type, BigDecimal.ZERO).add(dayStat.getDelta(type));
          saldoMap.put(type, saldo);
          dayStat.setSaldo(type, saldo);
        })
    );
  }

  /**
   * Заполняет карту экземпляров BsDayStat суммами из оборотов.
   */
  private void fillBsDayStatMap(Map<LocalDate, BsDayStat> map, List<Turnover> turnovers) {
    turnovers.forEach(t -> {
      BsDayStat dayStat = map.computeIfAbsent(t.getOperDate(), k -> new BsDayStat(t.getOperDate()));
      dayStat.setDelta(t.getFromAccType(), dayStat.getDelta(t.getFromAccType()).subtract(t.getAmount()));
      dayStat.setDelta(t.getToAccType(), dayStat.getDelta(t.getToAccType()).add(t.getAmount()));
      if (AccountType.income == t.getFromAccType()) {
        dayStat.setIncomeAmount(dayStat.getIncomeAmount().add(t.getAmount()));
      }
      if (AccountType.expense == t.getToAccType()) {
        dayStat.setChargeAmount(dayStat.getChargeAmount().add(t.getAmount()));
      }
    });
  }

  private List<Turnover> getRealTurnovers(UUID bsId, MoneyOperStatus status, LocalDate fromDate, LocalDate toDate) {
    return jdbcTemplate.query("" +
        "select trn_date as operDate, from_acc_type as fromAccType, to_acc_type as toAccType, " +
        "sum(amount) as amount " +
        "from v_trns_by_base_crn " +
        "where bs_id = ? and status = ? and trn_date between ? and ? " +
        "group by trn_date, from_acc_type, to_acc_type ",
        (rs, rowNum) -> {
          val operDate = rs.getObject("operDate", LocalDate.class);
          val fromType = AccountType.valueOf(rs.getString("fromAccType"));
          val toType = AccountType.valueOf(rs.getString("toAccType"));
          Turnover turnover = new Turnover(operDate, fromType, toType) {{
            setAmount(rs.getBigDecimal("amount"));
          }};
          return turnover;
        },
        bsId, status.name(), fromDate, toDate);
  }

  private List<Turnover> getTrendTurnovers(UUID bsId, LocalDate fromDate, LocalDate toDate) {
    return jdbcTemplate.query("" +
        "select trn_date as operDate, " +
        "from_acc_type as fromAccType, to_acc_type as toAccType, " +
        "sum(case when period = 'single' or recurrence_id is not null then 0 else amount end) as amount " +
        "from v_trns_by_base_crn " +
        "where bs_id = ? and status = ? and trn_date between ? and ? " +
        "group by trn_date, from_acc_type, to_acc_type ",
        (rs, rowNum) -> {
          val operDate = rs.getObject("operDate", LocalDate.class).plusMonths(1);
          val fromType = AccountType.valueOf(rs.getString("fromAccType"));
          val toType = AccountType.valueOf(rs.getString("toAccType"));
          Turnover turnover = new Turnover(operDate, fromType, toType) {{
            setAmount(rs.getBigDecimal("amount"));
          }};
          return turnover;
        },
        bsId, MoneyOperStatus.done.name(), fromDate, toDate);
  }

  private List<Turnover> getRecurrenceTurnovers(UUID bsId, LocalDate toDate) {
    BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
    Stream<RecurrenceOper> recurrenceOpers =  recurrenceOperRepo.findByBalanceSheet(balanceSheet);
    Set<Turnover> turnovers = new HashSet<>();
    LocalDate today = LocalDate.now();
    recurrenceOpers.forEach(ro -> {
      MoneyOper template = ro.getTemplate();
      LocalDate roNextDate = ro.getNextDate();
      while (roNextDate.isBefore(toDate)) {
        Account fromAcc = accountRepo.findOne(template.getFromAccId());
        Account toAcc = accountRepo.findOne(template.getToAccId());
        LocalDate nextDate = (roNextDate.isBefore(today)) ? today : roNextDate;
        Turnover newTurnover = new Turnover(nextDate, fromAcc.getType(), toAcc.getType());
        Optional<Turnover> turnover = turnovers.stream()
            .filter(t1 -> t1.equals(newTurnover))
            .findFirst();
        if (!turnover.isPresent()) {
          turnover = Optional.of(newTurnover);
          turnovers.add(newTurnover);
        }
        turnover.get().setAmount(turnover.get().getAmount().add(template.getAmount()));
        roNextDate = ro.calcNextDate(nextDate);
      }
    });
    return new ArrayList<>(turnovers);
  }

  private List<CategoryStat> getCategoies(UUID bsId, LocalDate fromDate, LocalDate toDate) {
    List<CategoryStat> list = jdbcTemplate.query("" +
            "SELECT" +
            "  c.id," +
            "  c.root_id AS rootId," +
            "  a.name," +
            "  sum(t.amount) as amount " +
            "FROM" +
            "  accounts a," +
            "  categories c" +
            "    LEFT JOIN v_trns_by_base_crn t ON t.to_acc_id = c.id AND t.trn_date between ? AND ? " +
            "WHERE a.balance_sheet_id = ? " +
            "  AND a.id = c.id AND a.type = 'expense' AND t.status = 'done' " +
            "GROUP BY c.id, a.name, coalesce(c.root_id, c.id)",
        (rs, rowNum) -> {
          UUID id = UUID.fromString(rs.getString("id"));
          String rootIdAsStr = rs.getString("rootId");
          UUID rootId = Objects.nonNull(rootIdAsStr) ? UUID.fromString(rootIdAsStr) : null;
          return new CategoryStat(id, rootId, rs.getString("name"), rs.getBigDecimal("amount"));
        },
        fromDate, toDate, bsId);
    list.stream()
        .filter(cs -> cs.getRootId() == null)
        .forEach(root -> {
          Double sum = list.stream()
              .filter(cs -> Objects.equals(cs.getRootId(), root.getId()))
              .mapToDouble(cs -> cs.getAmount().doubleValue())
              .reduce(0d, (s, d) -> s += d);
          root.setAmount(root.getAmount().add(new BigDecimal(sum)));
        });
    return list.stream()
        .filter(cs -> Objects.isNull(cs.getRootId()))
        .sorted((cs1, cs2) -> cs1.getAmount().compareTo(cs2.getAmount()) * -1)
        .collect(Collectors.toList());
  }

}
