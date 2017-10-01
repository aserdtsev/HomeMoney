package ru.serdtsev.homemoney.moneyoper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.serdtsev.homemoney.account.Account;
import ru.serdtsev.homemoney.account.AccountRepository;
import ru.serdtsev.homemoney.account.Balance;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;

/**
 * Предоставляет методы работы с денежными операциями
 */
@Service
public class MoneyOperService {
  private static final String SEARCH_DATE_REGEX = "\\p{Digit}{4}-\\p{Digit}{2}-\\p{Digit}{2}";
  private static final String SEARCH_UUID_REGEX = "\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}";
  private static final String SEARCH_MONEY_REGEX = "\\p{Digit}+\\.*\\p{Digit}*";
  private final MoneyOperRepository moneyOperRepo;
  private final AccountRepository accountRepo;

  @Autowired
  public MoneyOperService(MoneyOperRepository moneyOperRepo, AccountRepository accountRepo) {
    this.moneyOperRepo = moneyOperRepo;
    this.accountRepo = accountRepo;
  }

  /**
   * Возвращает следующие повторяющиеся платежи.
   */
  Stream<MoneyOper> getNextRecurrenceOpers(BalanceSheet balanceSheet, String search, Date beforeDate) {
    return moneyOperRepo.findByBalanceSheetAndIsTemplate(balanceSheet, true)
        .filter(oper -> isTemplateMatchSearch(oper, search) && oper.getNextDate().before(beforeDate))
        .map(recurrenceOper -> createMoneyOperByTemplate(balanceSheet, recurrenceOper))
        .sorted(Comparator.comparing(MoneyOper::getPerformed).reversed());
  }

  /**
   * Возвращает последние повторяющиеся платежи.
   */
  public Stream<MoneyOper> getLastRecurrenceOpers(BalanceSheet balanceSheet, String search) {
    return moneyOperRepo.findByBalanceSheetAndIsTemplate(balanceSheet, true)
        .filter(oper -> isTemplateMatchSearch(oper, search))
        .sorted(Comparator.comparing(MoneyOper::getNextDate));
  }

  /**
   * @return true, если шаблон операции соответствует строке поиска
   */
  private boolean isTemplateMatchSearch(MoneyOper templateOper, String search) {
    if (StringUtils.isEmpty(search)) {
      return true;
    } else if (search.matches(SEARCH_DATE_REGEX)) {
      // по дате в формате ISO
      return templateOper.getNextDate().compareTo(Date.valueOf(search)) == 0;
    } else if (search.matches(SEARCH_UUID_REGEX)) {
      // по идентификатору операции
      return templateOper.getId().equals(UUID.fromString(search));
    } else if (search.matches(SEARCH_MONEY_REGEX)) {
      // по сумме операции
      return templateOper.getBalanceChanges()
          .stream()
          .anyMatch(change -> change.getValue().plus().compareTo(new BigDecimal(search)) == 0);
    } else {
      return templateOper.getBalanceChanges()
          .stream()
          .anyMatch(change -> change.getBalance().getName().toLowerCase().contains(search))
          || templateOper.getComment().toLowerCase().contains(search)
          || templateOper.getLabels().stream().anyMatch(label -> label.getName().toLowerCase().contains(search));
    }
  }

  private MoneyOper createMoneyOperByTemplate(BalanceSheet balanceSheet, MoneyOper templateOper) {
    MoneyOper oper = new MoneyOper(UUID.randomUUID(), balanceSheet, MoneyOperStatus.recurrence,
        templateOper.getNextDate(), 0, templateOper.getLabels(), templateOper.getComment(), templateOper.getPeriod());
    oper.addBalanceChanges(templateOper.getBalanceChanges());
    oper.setFromAccId(templateOper.getFromAccId());
    oper.setAmount(templateOper.getAmount());
    oper.setToAccId(templateOper.getToAccId());
    oper.setToAmount(templateOper.getToAmount());
    oper.setRecurrenceId(templateOper.getRecurrenceId());
    oper.setTemplateId(templateOper.getId());
    return oper;
  }

  public void deleteRecurrenceOper(BalanceSheet balanceSheet, UUID recurrenceId) {
    Optional.ofNullable(moneyOperRepo.findByBalanceSheetAndRecurrenceIdAndIsTemplate(balanceSheet, recurrenceId, true))
        .ifPresent(oper -> {
          oper.setTemplate(false);
          oper.setNextDate(null);
          moneyOperRepo.save(oper);
        });
  }

  /**
   * Создает экземпляр MoneyOper.
   */
  public MoneyOper newMoneyOper(BalanceSheet balanceSheet, UUID moneyOperId, MoneyOperStatus status, Date performed,
      Integer dateNum, List<Label> labels, String comment, Period period, UUID fromAccId, UUID toAccId, BigDecimal amount,
      BigDecimal toAmount, UUID parentId, MoneyOper templateOper) {
    MoneyOper oper = new MoneyOper(moneyOperId, balanceSheet, status, performed, dateNum, labels, comment, period);
    if (nonNull(templateOper)) {
      oper.setTemplateId(templateOper.getId());
      oper.setRecurrenceId(templateOper.getRecurrenceId());
    }

    oper.setFromAccId(fromAccId);
    Account fromAcc = accountRepo.findOne(fromAccId);
    assert fromAcc != null;
    if (fromAcc instanceof Balance) {
      oper.addBalanceChange((Balance) fromAcc, amount.negate(), performed);
    }

    oper.setToAccId(toAccId);
    Account toAcc = accountRepo.findOne(toAccId);
    assert toAcc != null;
    if (toAcc instanceof  Balance) {
      oper.addBalanceChange((Balance) toAcc, toAmount, performed);
    }

    oper.setAmount(amount);
    oper.setToAmount(toAmount);

    if (parentId != null) {
      MoneyOper parentOper =  moneyOperRepo.findOne(parentId);
      assert parentOper != null;
      oper.setParentOper(parentOper);
    }

    return oper;
  }

  public void save(MoneyOper moneyOper) {
    moneyOperRepo.save(moneyOper);
  }
}
