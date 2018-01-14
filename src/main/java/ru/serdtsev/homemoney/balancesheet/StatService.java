package ru.serdtsev.homemoney.balancesheet;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.serdtsev.homemoney.account.AccountRepository;
import ru.serdtsev.homemoney.account.AccountType;
import ru.serdtsev.homemoney.dto.*;
import ru.serdtsev.homemoney.moneyoper.MoneyOperItemRepo;
import ru.serdtsev.homemoney.moneyoper.MoneyOperRepo;
import ru.serdtsev.homemoney.moneyoper.RecurrenceOperRepo;
import ru.serdtsev.homemoney.moneyoper.model.*;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class StatService {
  private final BalanceSheetRepository balanceSheetRepo;
  private final RecurrenceOperRepo recurrenceOperRepo;
  private final AccountRepository accountRepo;
  private final MoneyOperRepo moneyOperRepo;
  private final MoneyOperItemRepo moneyOperItemRepo;
  private final DataSource dataSource;
  private final JdbcTemplate jdbcTemplate;

  public BsStat getBsStat(UUID bsId, Long interval) {
    BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);

    LocalDate today = LocalDate.now();
    LocalDate toDate = today;
    LocalDate fromDate = today.minusDays(interval);

    BsStat bsStat = new BsStat(bsId, fromDate, toDate);
    calcCurrentSaldo(bsStat);

    TreeMap<LocalDate, BsDayStat> map = new TreeMap<>();
    Collection<Turnover> realTurnovers = getRealTurnovers(balanceSheet, MoneyOperStatus.done, fromDate, toDate);
    fillBsDayStatMap(map, realTurnovers);
    calcPastSaldoAndTurnovers(bsStat, map);

    LocalDate trendFromDate = today.plusDays(1).minusMonths(1);
    LocalDate trendToDate = trendFromDate.plusDays(interval - 1);
    TreeMap<LocalDate, BsDayStat> trendMap = new TreeMap<>();
    fillBsDayStatMap(trendMap, getTrendTurnovers(balanceSheet, trendFromDate, trendToDate));
    fillBsDayStatMap(trendMap, getRealTurnovers(balanceSheet, MoneyOperStatus.pending, LocalDate.of(1970, 1, 1), today.plusDays(interval)));
    fillBsDayStatMap(trendMap, getRecurrenceTurnovers(balanceSheet, today.plusDays(interval)));
    calcTrendSaldoAndTurnovers(bsStat, trendMap);

    map.putAll(trendMap);
    bsStat.setDayStats(new ArrayList<>(map.values()));
    bsStat.setCategories(getCategories(balanceSheet, fromDate, toDate));

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

  private void calcPastSaldoAndTurnovers(BsStat bsStat, Map<LocalDate, BsDayStat> map) {
    Map<AccountType, BigDecimal> saldoMap = new HashMap<>(AccountType.values().length);
    bsStat.getSaldoMap().forEach((type, value) -> saldoMap.put(type, value.plus()));
    List<BsDayStat> dayStats = new ArrayList<>(map.values());
    dayStats.sort((e1, e2) -> (e1.getLocalDate().isAfter(e2.getLocalDate())) ? -1 : 1);
    dayStats.forEach(dayStat -> {
      Arrays.asList(AccountType.values()).forEach(type -> {
        BigDecimal saldo = saldoMap.getOrDefault(type, BigDecimal.ZERO);
        dayStat.setSaldo(type, saldo);
        saldoMap.put(type, saldo.subtract(dayStat.getDelta(type)));
      });
      bsStat.setIncomeAmount(bsStat.getIncomeAmount().add(dayStat.getIncomeAmount()));
      bsStat.setChargesAmount(bsStat.getChargesAmount().add(dayStat.getChargeAmount()));
    });
  }

  private void calcTrendSaldoAndTurnovers(BsStat bsStat, Map<LocalDate, BsDayStat> trendMap) {
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
  private void fillBsDayStatMap(Map<LocalDate, BsDayStat> map, Collection<Turnover> turnovers) {
    turnovers.forEach(t -> {
      BsDayStat dayStat = map.computeIfAbsent(t.getOperDate(), k -> new BsDayStat(t.getOperDate()));
      dayStat.setDelta(t.getAccountType(), dayStat.getDelta(t.getAccountType()).add(t.getAmount()));
      if (t.getAccountType() == AccountType.income) {
        dayStat.setIncomeAmount(dayStat.getIncomeAmount().add(t.getAmount()));
      } else if (t.getAccountType() == AccountType.expense) {
        dayStat.setChargeAmount(dayStat.getChargeAmount().add(t.getAmount()));
      }
    });
  }

  private Collection<Turnover> getRealTurnovers(BalanceSheet balanceSheet, MoneyOperStatus status, LocalDate fromDate, LocalDate toDate) {
    Map<Turnover, List<Turnover>> turnovers = moneyOperItemRepo.findByBalanceSheetAndPerformedBetween(balanceSheet, fromDate, toDate)
        .filter(item -> item.getMoneyOper().getStatus() == status)
        .flatMap(item -> {
          List<Turnover> list = new ArrayList<>();

          Turnover t = new Turnover(item.getPerformed(), item.getBalance().getType(), item.getValue());
          list.add(t);

          MoneyOper oper = item.getMoneyOper();
          if (oper.getType() != MoneyOperType.transfer) {
            list.add(new Turnover(item.getPerformed(), AccountType.valueOf(oper.getType().name()), item.getValue().abs()));
          }
          return list.stream();
        })
        .collect(groupingBy(t -> new Turnover(t.getOperDate(), t.getAccountType()), toList()));

    turnovers.forEach((t, list) ->
        list.forEach(ti -> t.plus(ti.getAmount())));

    return turnovers.keySet();
  }

  private Collection<Turnover> getTrendTurnovers(BalanceSheet balanceSheet, LocalDate fromDate, LocalDate toDate) {
    Map<Turnover, List<Turnover>> turnovers = moneyOperItemRepo.findByBalanceSheetAndPerformedBetween(balanceSheet, fromDate, toDate)
        .filter(item -> {
          MoneyOper oper = item.getMoneyOper();
          Period period = oper.getPeriod();
          return oper.getStatus() == MoneyOperStatus.done && period == Period.month && isNull(oper.getRecurrenceId());
        })
        .flatMap(item -> {
          List<Turnover> list = new ArrayList<>();

          MoneyOper oper = item.getMoneyOper();
          LocalDate trendDate = item.getPerformed().plusMonths(1);
          assert trendDate.isAfter(toDate) : item;
          Turnover t = new Turnover(trendDate, item.getBalance().getType(), item.getValue());
          list.add(t);

          if (oper.getType() != MoneyOperType.transfer) {
            list.add(new Turnover(trendDate, AccountType.valueOf(oper.getType().name()), item.getValue().abs()));
          }
          return list.stream();
        })
        .collect(groupingBy(t -> new Turnover(t.getOperDate(), t.getAccountType()), toList()));

    turnovers.forEach((t, list) ->
        list.forEach(ti -> t.plus(ti.getAmount())));

    return turnovers.keySet();
  }

  private LocalDate getTrendDate(LocalDate performed, Period period) {
    return period == Period.month ? performed.plusMonths(1)
                : period == Period.quarter ? performed.plusMonths(3)
                : period == Period.year ? performed.plusYears(1)
                : LocalDate.MAX;
  }

  private Collection<Turnover> getRecurrenceTurnovers(BalanceSheet balanceSheet, LocalDate toDate) {
    Stream<RecurrenceOper> recurrenceOpers =  recurrenceOperRepo.findByBalanceSheet(balanceSheet);
    Set<Turnover> turnovers = new HashSet<>();
    LocalDate today = LocalDate.now();
    recurrenceOpers.forEach(ro -> {
      MoneyOper template = ro.getTemplate();
      LocalDate roNextDate = ro.getNextDate();
      while (roNextDate.isBefore(toDate)) {
        LocalDate nextDate = (roNextDate.isBefore(today)) ? today : roNextDate;
        template.getItems().forEach(item -> {
          putRecurrenceTurnover(turnovers, item.getValue(), item.getBalance().getType(), nextDate);
          MoneyOperType operType = template.getType();
          if (operType != MoneyOperType.transfer) {
            putRecurrenceTurnover(turnovers, item.getValue().abs(), AccountType.valueOf(operType.name()), nextDate);
          }
        });
        roNextDate = ro.calcNextDate(nextDate);
      }
    });
    return new ArrayList<>(turnovers);
  }

  private void putRecurrenceTurnover(Set<Turnover> turnovers, BigDecimal amount, AccountType accountType, LocalDate nextDate) {
    Turnover turnover = new Turnover(nextDate, accountType);
    Optional<Turnover> turnoverOpt = turnovers.stream()
        .filter(t1 -> t1.equals(turnover))
        .findFirst();
    if (!turnoverOpt.isPresent()) {
      turnoverOpt = Optional.of(turnover);
      turnovers.add(turnover);
    }
    turnoverOpt.get().setAmount(turnoverOpt.get().getAmount().add(amount));
  }

  private List<CategoryStat> getCategories(BalanceSheet balanceSheet, LocalDate fromDate, LocalDate toDate) {
    UUID absentCatId = UUID.randomUUID();
    Map<CategoryStat, List<CategoryStat>> map = moneyOperItemRepo.findByBalanceSheetAndPerformedBetween(balanceSheet, fromDate, toDate)
        .filter(item -> {
          MoneyOper oper = item.getMoneyOper();
          return oper.getType() == MoneyOperType.expense && oper.getStatus() == MoneyOperStatus.done;
        })
        .map(item -> {
          MoneyOper oper = item.getMoneyOper();
          Optional<Label> catOpt = oper.getLabels().stream()
              .filter(Label::getIsCategory)
              .findAny();
          UUID id = catOpt.isPresent() ? catOpt.get().getId() : absentCatId;
          UUID rootId = catOpt.isPresent() ? catOpt.get().getRootId() : null;
          String name = catOpt.isPresent() ? catOpt.get().getName() : "<Без категории>";
          return new CategoryStat(id, rootId, name, item.getValue().abs());
        })
        .collect(groupingBy(cat -> cat));

     map.forEach((c, l) -> {
          BigDecimal sum = l.stream()
              .map(CategoryStat::getAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          c.setAmount(sum);
        });

    List<CategoryStat> list = new ArrayList<>(map.keySet());

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
