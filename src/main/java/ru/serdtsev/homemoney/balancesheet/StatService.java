package ru.serdtsev.homemoney.balancesheet;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.serdtsev.homemoney.account.model.AccountType;
import ru.serdtsev.homemoney.dto.*;
import ru.serdtsev.homemoney.moneyoper.LabelRepository;
import ru.serdtsev.homemoney.moneyoper.MoneyOperItemRepo;
import ru.serdtsev.homemoney.moneyoper.model.Label;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOper;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Service
@RequiredArgsConstructor
@Slf4j
@Repository
@Transactional(readOnly = true)
public class StatService {
  private final BalanceSheetRepository balanceSheetRepo;
  private final LabelRepository labelRepository;
  private final MoneyOperItemRepo moneyOperItemRepo;
  private final JdbcTemplate jdbcTemplate;
  private final StatData statData;

  @SneakyThrows
  public BsStat getBsStat(UUID bsId, Long interval) {
    BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);

    LocalDate today = LocalDate.now();
    LocalDate toDate = today;
    LocalDate fromDate = today.minusDays(interval);

    BsStat bsStat = new BsStat(bsId, fromDate, toDate);
    calcCurrentSaldo(bsStat);

    LocalDate trendFromDate = today.plusDays(1).minusMonths(1);
    LocalDate trendToDate = trendFromDate.plusDays(interval - 1);

    TreeMap<LocalDate, BsDayStat> trendMap = new TreeMap<>();

    CompletableFuture<Collection<Turnover>> realTurnovers = statData.getRealTurnoversFuture(balanceSheet, MoneyOperStatus.done, fromDate, toDate);
    CompletableFuture<Collection<Turnover>> pendingTurnovers = statData.getRealTurnoversFuture(balanceSheet, MoneyOperStatus.pending,
        LocalDate.of(1970, 1, 1), today.plusDays(interval));
    CompletableFuture<Collection<Turnover>> trendTurnovers = statData.getTrendTurnoversFuture(balanceSheet, trendFromDate, trendToDate);
    CompletableFuture<Collection<Turnover>> recurrenceTurnovers = statData.getRecurrenceTurnoversFuture(balanceSheet, today.plusDays(interval));

    TreeMap<LocalDate, BsDayStat> map = new TreeMap<>();
    fillBsDayStatMap(map, realTurnovers.get());
    calcPastSaldoAndTurnovers(bsStat, map);

    fillBsDayStatMap(trendMap, trendTurnovers.get());
    fillBsDayStatMap(trendMap, pendingTurnovers.get());
    fillBsDayStatMap(trendMap, recurrenceTurnovers.get());
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
      } else if (t.getAccountType() == AccountType.expense || t.getAccountType() == AccountType.reserve) {
        dayStat.setChargeAmount(dayStat.getChargeAmount().add(t.getAmount()));
      }
    });
  }

  @SneakyThrows
  private List<CategoryStat> getCategories(BalanceSheet balanceSheet, LocalDate fromDate, LocalDate toDate) {
    UUID absentCatId = UUID.randomUUID();
    Map<CategoryStat, List<CategoryStat>> map = moneyOperItemRepo.findByBalanceSheetAndPerformedBetweenAndMoneyOperStatus(balanceSheet,
        fromDate, toDate, MoneyOperStatus.done)
        .stream()
        .filter(item -> item.getMoneyOper().getType() == MoneyOperType.expense || item.getBalance().getType() == AccountType.reserve)
        .map(item -> {
          MoneyOper oper = item.getMoneyOper();
          Optional<Label> catOpt = oper.getLabels().stream()
              .filter(Label::getIsCategory)
              .findFirst();
          UUID rootId = catOpt.map(Label::getRootId).orElse(null);
          if (rootId != null) {
            catOpt = Optional.of(labelRepository.findOne(rootId));
          }

          boolean isReserveIncrease = item.getBalance().getType() == AccountType.reserve && item.getValue().signum() > 0;

          UUID id = catOpt.isPresent()
                  ? catOpt.get().getId()
                  : isReserveIncrease ? item.getBalance().getId() : absentCatId;
          String name = catOpt.isPresent()
                  ? catOpt.get().getName()
                  : isReserveIncrease ? item.getBalance().getName() : "<Без категории>";
          return new CategoryStat(id, null, name, item.getValue().abs());
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
