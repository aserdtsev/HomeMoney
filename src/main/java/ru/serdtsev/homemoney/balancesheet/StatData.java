package ru.serdtsev.homemoney.balancesheet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.serdtsev.homemoney.account.AccountRepository;
import ru.serdtsev.homemoney.account.model.AccountType;
import ru.serdtsev.homemoney.dto.Turnover;
import ru.serdtsev.homemoney.moneyoper.MoneyOperItemRepo;
import ru.serdtsev.homemoney.moneyoper.MoneyOperRepo;
import ru.serdtsev.homemoney.moneyoper.RecurrenceOperRepo;
import ru.serdtsev.homemoney.moneyoper.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Component
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatData {
  private final MoneyOperItemRepo moneyOperItemRepo;
  private final RecurrenceOperRepo recurrenceOperRepo;

  @Async
  public CompletableFuture<Collection<Turnover>> getRealTurnoversFuture(BalanceSheet balanceSheet, MoneyOperStatus status, LocalDate fromDate, LocalDate toDate) {
    return CompletableFuture.completedFuture(getRealTurnovers(balanceSheet, status, fromDate, toDate));
  }

  Collection<Turnover> getRealTurnovers(BalanceSheet balanceSheet, MoneyOperStatus status, LocalDate fromDate, LocalDate toDate) {
    log.info("getRealTurnovers start by {}, {} - {}", status,
        fromDate.format(DateTimeFormatter.ISO_DATE), toDate.format(DateTimeFormatter.ISO_DATE));
    Map<Turnover, List<Turnover>> turnovers = moneyOperItemRepo.findByBalanceSheetAndPerformedBetweenAndMoneyOperStatus(balanceSheet,
        fromDate, toDate, status)
        .stream()
        .filter(item -> item.getMoneyOper().getStatus() == status)
        .filter(item -> item.getBalance().getType().isTurnover())
        .flatMap(item -> {
          List<Turnover> list = new ArrayList<>();

          Turnover t = new Turnover(item.getPerformed(), item.getBalance().getType(), item.getValue());
          list.add(t);

          MoneyOper oper = item.getMoneyOper();
          if (oper.getType() != MoneyOperType.transfer) {
            AccountType accountType = AccountType.valueOf(oper.getType().name());
            list.add(new Turnover(item.getPerformed(), accountType, item.getValue().abs()));
          }
          return list.stream();
        })
        .collect(groupingBy(t -> new Turnover(t.getOperDate(), t.getAccountType()), toList()));

    turnovers.forEach((t, list) ->
        list.forEach(ti -> t.plus(ti.getAmount())));

    log.info("getRealTurnovers finish");
    return turnovers.keySet();
  }

  @Async
  public CompletableFuture<Collection<Turnover>> getTrendTurnoversFuture(BalanceSheet balanceSheet, LocalDate fromDate, LocalDate toDate) {
    return CompletableFuture.completedFuture(getTrendTurnovers(balanceSheet, fromDate, toDate));
  }

  private Collection<Turnover> getTrendTurnovers(BalanceSheet balanceSheet, LocalDate fromDate, LocalDate toDate) {
    log.info("getTrendTurnovers start");
    Map<Turnover, List<Turnover>> turnovers = moneyOperItemRepo.findByBalanceSheetAndPerformedBetweenAndMoneyOperStatus(balanceSheet,
        fromDate, toDate, MoneyOperStatus.done)
        .stream()
        .filter(item -> item.getMoneyOper().getPeriod() == Period.month && isNull(item.getMoneyOper().getRecurrenceId()))
        .filter(item -> item.getBalance().getType().isTurnover())
        .flatMap(item -> {
          List<Turnover> list = new ArrayList<>();

          MoneyOper oper = item.getMoneyOper();
          LocalDate trendDate = item.getPerformed().plusMonths(1);
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

    val list = new ArrayList<Turnover>(turnovers.keySet());
    list.sort(Comparator.comparing(Turnover::getOperDate));
    log.info("getTrendTurnovers finish");
    return list;
  }

  private LocalDate getTrendDate(LocalDate performed, Period period) {
    return period == Period.month ? performed.plusMonths(1)
        : period == Period.quarter ? performed.plusMonths(3)
        : period == Period.year ? performed.plusYears(1)
        : LocalDate.MAX;
  }

  public CompletableFuture<Collection<Turnover>> getRecurrenceTurnoversFuture(BalanceSheet balanceSheet, LocalDate toDate) {
    return CompletableFuture.completedFuture(getRecurrenceTurnovers(balanceSheet, toDate));
  }

  private Collection<Turnover> getRecurrenceTurnovers(BalanceSheet balanceSheet, LocalDate toDate) {
    log.info("getRecurrenceTurnovers start");
    Stream<RecurrenceOper> recurrenceOpers = recurrenceOperRepo.findByBalanceSheet(balanceSheet);
    Set<Turnover> turnovers = new HashSet<>();
    LocalDate today = LocalDate.now();
    recurrenceOpers
        .filter(ro -> !ro.getArc())
        .forEach(ro -> {
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
    val list = new ArrayList<Turnover>(turnovers);
    list.sort(Comparator.comparing(Turnover::getOperDate));
    log.info("getRecurrenceTurnovers finish");
    return list;
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

}
