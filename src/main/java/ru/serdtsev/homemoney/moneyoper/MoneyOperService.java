package ru.serdtsev.homemoney.moneyoper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Предоставляет методы работы с денежными операциями
 */
@Service
public class MoneyOperService {
  private static final String SEARCH_DATE_REGEX = "\\p{Digit}{4}-\\p{Digit}{2}-\\p{Digit}{2}";
  private static final String SEARCH_UUID_REGEX = "\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}";
  private static final String SEARCH_MONEY_REGEX = "\\p{Digit}+\\.*\\p{Digit}*";
  private final MoneyOperRepository moneyOperRepo;

  @Autowired
  public MoneyOperService(MoneyOperRepository moneyOperRepo) {
    this.moneyOperRepo = moneyOperRepo;
  }

  public Stream<MoneyOper> getRecurrenceOpers(BalanceSheet balanceSheet, String search, Date beforeDate) {
    return moneyOperRepo.findByBalanceSheetAndIsTemplate(balanceSheet, true)
        .filter(oper -> isTemplateMatchSearch(oper, search) && oper.getNextDate().before(beforeDate))
        .map(recurrenceOper -> createMoneyOperByTemplate(balanceSheet, recurrenceOper))
        .sorted(Comparator.comparing(MoneyOper::getPerformed).reversed());
  }

  public Stream<MoneyOper> getRecurrenceOpers(BalanceSheet balanceSheet, String search) {
    return moneyOperRepo.findByBalanceSheetAndIsTemplate(balanceSheet, true)
        .filter(oper -> isTemplateMatchSearch(oper, search))
        .map(recurrenceOper -> createMoneyOperByTemplate(balanceSheet, recurrenceOper))
        .sorted(Comparator.comparing(MoneyOper::getPerformed));
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
    oper.setTemplateOper(templateOper);
    return oper;
  }

}
