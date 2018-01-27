package ru.serdtsev.homemoney.moneyoper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.serdtsev.homemoney.account.Account;
import ru.serdtsev.homemoney.account.AccountRepository;
import ru.serdtsev.homemoney.account.Balance;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.moneyoper.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.isTrue;

/**
 * Предоставляет методы работы с денежными операциями
 */
@Service
public class MoneyOperService {
  private static final String SEARCH_DATE_REGEX = "\\p{Digit}{4}-\\p{Digit}{2}-\\p{Digit}{2}";
  private static final String SEARCH_UUID_REGEX = "\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}";
  private static final String SEARCH_MONEY_REGEX = "\\p{Digit}+\\.*\\p{Digit}*";
  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final BalanceSheetRepository balanceSheetRepo;
  private final MoneyOperRepo moneyOperRepo;
  private final RecurrenceOperRepo recurrenceOperRepo;
  private final AccountRepository accountRepo;
  private final LabelRepository labelRepo;

  @Autowired
  public MoneyOperService(BalanceSheetRepository balanceSheetRepo, MoneyOperRepo moneyOperRepo, RecurrenceOperRepo recurrenceOperRepo,
      AccountRepository accountRepo, LabelRepository labelRepo) {
    this.balanceSheetRepo = balanceSheetRepo;
    this.moneyOperRepo = moneyOperRepo;
    this.recurrenceOperRepo = recurrenceOperRepo;
    this.accountRepo = accountRepo;
    this.labelRepo = labelRepo;
  }

  public Optional<MoneyOper> findMoneyOper(UUID id) {
    return Optional.ofNullable(moneyOperRepo.findOne(id));
  }

  public void save(MoneyOper moneyOper) {
    moneyOperRepo.save(moneyOper);
  }

  public Optional<RecurrenceOper> findRecurrenceOper(UUID id) {
    return Optional.ofNullable(recurrenceOperRepo.findOne(id));
  }

  public void save(RecurrenceOper recurrenceOper) {
    recurrenceOperRepo.save(recurrenceOper);
  }

  /**
   * Возвращает следующие повторы операций.
   */
  Stream<MoneyOper> getNextRecurrenceOpers(BalanceSheet balanceSheet, String search, LocalDate beforeDate) {
    return getRecurrenceOpers(balanceSheet, search)
        .filter(recurrenceOper -> recurrenceOper.getNextDate().isBefore(beforeDate))
        .map(recurrenceOper -> createMoneyOperByTemplate(balanceSheet, recurrenceOper));
  }

  /**
   * Возвращает повторяющиеся операции.
   */
  public Stream<RecurrenceOper> getRecurrenceOpers(BalanceSheet balanceSheet, String search) {
    return recurrenceOperRepo.findByBalanceSheet(balanceSheet)
        .filter(recurrenceOper -> !recurrenceOper.getArc() && isOperMatchSearch(recurrenceOper, search));
  }

  /**
   * @return true, если шаблон операции соответствует строке поиска
   */
  private boolean isOperMatchSearch(RecurrenceOper recurrenceOper, String search) {
    MoneyOper template = recurrenceOper.getTemplate();
    if (StringUtils.isEmpty(search)) {
      return true;
    } else if (search.matches(SEARCH_DATE_REGEX)) {
      // по дате в формате ISO
      return recurrenceOper.getNextDate().isEqual(LocalDate.parse(search));
    } else if (search.matches(SEARCH_UUID_REGEX)) {
      // по идентификатору операции
      return template.getId().equals(UUID.fromString(search));
    } else if (search.matches(SEARCH_MONEY_REGEX)) {
      // по сумме операции
      return template.getItems()
          .stream()
          .anyMatch(change -> change.getValue().plus().compareTo(new BigDecimal(search)) == 0);
    } else {
      return template.getItems()
          .stream()
          .anyMatch(change -> change.getBalance().getName().toLowerCase().contains(search))
          || template.getComment().toLowerCase().contains(search)
          || template.getLabels().stream().anyMatch(label -> label.getName().toLowerCase().contains(search));
    }
  }

  private MoneyOper createMoneyOperByTemplate(BalanceSheet balanceSheet, RecurrenceOper recurrenceOper) {
    MoneyOper template = recurrenceOper.getTemplate();
    MoneyOper oper = new MoneyOper(UUID.randomUUID(), balanceSheet, MoneyOperStatus.recurrence,
        recurrenceOper.getNextDate(), 0, template.getLabels(), template.getComment(), template.getPeriod());
    oper.addItems(template.getItems());
    oper.setFromAccId(template.getFromAccId());
    oper.setAmount(template.getAmount());
    oper.setToAccId(template.getToAccId());
    oper.setToAmount(template.getToAmount());
    oper.setRecurrenceId(template.getRecurrenceId());
    return oper;
  }

  public void createRecurrenceOper(BalanceSheet balanceSheet, UUID operId) {
    MoneyOper sample = moneyOperRepo.findOne(operId);
    requireNonNull(sample);
    checkMoneyOperBelongsBalanceSheet(sample, balanceSheet.getId());
    MoneyOper template = newMoneyOper(balanceSheet, UUID.randomUUID(), MoneyOperStatus.template, null, null,
        sample.getLabels(), sample.getComment(), sample.getPeriod(), sample.getFromAccId(), sample.getToAccId(), sample.getAmount(),
        sample.getToAmount(), null, null);
    RecurrenceOper recurrenceOper = new RecurrenceOper(UUID.randomUUID(), balanceSheet, template, sample.getPerformed());
    recurrenceOper.skipNextDate();
    moneyOperRepo.save(template);
    recurrenceOperRepo.save(recurrenceOper);

    // todo поправить эту косоту
    template.setRecurrenceId(recurrenceOper.getId());
    moneyOperRepo.save(template);
    sample.setRecurrenceId(recurrenceOper.getId());
    moneyOperRepo.save(sample);
  }

  public void deleteRecurrenceOper(BalanceSheet balanceSheet, UUID recurrenceId) {
    RecurrenceOper recurrenceOper = recurrenceOperRepo.findOne(recurrenceId);
    recurrenceOper.arc();
    recurrenceOperRepo.save(recurrenceOper);
    log.info("RecurrenceOper '{}' moved to archive.", recurrenceId);
  }

  public void skipRecurrenceOper(BalanceSheet balanceSheet, UUID recurrenceId) {
    RecurrenceOper recurrenceOper = recurrenceOperRepo.findOne(recurrenceId);
    recurrenceOper.skipNextDate();
    recurrenceOperRepo.save(recurrenceOper);
  }

  /**
   * Создает экземпляр MoneyOper.
   */
  public MoneyOper newMoneyOper(BalanceSheet balanceSheet, UUID moneyOperId, MoneyOperStatus status, LocalDate performed,
      Integer dateNum, Collection<Label> labels, String comment, Period period, UUID fromAccId, UUID toAccId, BigDecimal amount,
      BigDecimal toAmount, UUID parentId, MoneyOper templateOper) {
    MoneyOper oper = new MoneyOper(moneyOperId, balanceSheet, status, performed, dateNum, labels, comment, period);
    if (nonNull(templateOper)) {
      oper.setRecurrenceId(templateOper.getRecurrenceId());
    }

    oper.setFromAccId(fromAccId);
    Account fromAcc = accountRepo.findOne(fromAccId);
    assert fromAcc != null;
    if (fromAcc instanceof Balance) {
      oper.addItem((Balance) fromAcc, amount.negate(), performed);
    }

    oper.setToAccId(toAccId);
    Account toAcc = accountRepo.findOne(toAccId);
    assert toAcc != null;
    if (toAcc instanceof  Balance) {
      oper.addItem((Balance) toAcc, toAmount, performed);
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

  public void updateRecurrenceOper(BalanceSheet balanceSheet, RecurrenceOperDto recurrenceOperDto) {
    RecurrenceOper recurrenceOper = recurrenceOperRepo.findOne(recurrenceOperDto.getId());
    recurrenceOper.setNextDate(recurrenceOperDto.getNextDate());
    MoneyOper template = recurrenceOper.getTemplate();

    updateFromAccount(template, recurrenceOperDto.getFromAccId());
    updateToAccount(template, recurrenceOperDto.getToAccId());
    updateAmount(template, template.getAmount());
    updateToAmount(template, template.getToAmount());

    template.setComment(recurrenceOperDto.getComment());

    Collection<Label> labels = getLabelsByStrings(balanceSheet, recurrenceOperDto.getLabels());
    template.setLabels(labels);

    recurrenceOperRepo.save(recurrenceOper);
  }

  void updateAmount(MoneyOper oper, BigDecimal amount) {
    if (oper.getAmount().compareTo(amount) == 0) return;
    oper.getItems().stream()
        .filter(item -> item.getValue().signum() < 0)
        .forEach(item -> item.setValue(amount.negate()));
    oper.setAmount(amount);
  }

  void updateToAmount(MoneyOper oper, BigDecimal amount) {
    if (oper.getToAmount().compareTo(amount) == 0) return;
    oper.getItems().stream()
        .filter(item -> item.getValue().signum() > 0)
        .forEach(item -> item.setValue(amount));
    oper.setToAmount(amount);
  }

  void updateFromAccount(MoneyOper oper, UUID accId) {
    if (oper.getFromAccId().equals(accId)) return;
    replaceBalance(oper, oper.getFromAccId(), accId);
    oper.setFromAccId(accId);
  }

  private void replaceBalance(MoneyOper oper, UUID oldAccId, UUID newAccId) {
    oper.getItems().stream()
        .filter(item -> item.getBalance().getId().equals(oldAccId))
        .forEach(item -> {
          Balance balance = (Balance) accountRepo.findOne(newAccId);
          item.setBalance(balance);
        });
  }

  void updateToAccount(MoneyOper oper, UUID accId) {
    if (oper.getToAccId().equals(accId)) return;
    replaceBalance(oper, oper.getToAccId(), accId);
    oper.setToAccId(accId);
  }

  public void checkMoneyOperBelongsBalanceSheet(MoneyOper oper, UUID bsId) {
    isTrue(Objects.equals(oper.getBalanceSheet().getId(), bsId),
        format("MoneyOper id='%s' belongs the other balance sheet.", oper.getId()));
  }

  public List<Label> getLabelsByStrings(BalanceSheet balanceSheet, List<String> strLabels) {
    return strLabels
        .stream()
        .map(name -> findOrCreateLabel(balanceSheet, name))
        .collect(Collectors.toList());
  }

  Label findOrCreateLabel(BalanceSheet balanceSheet, String name) {
    Label label = labelRepo.findByBalanceSheetAndName(balanceSheet, name);
    return Optional.ofNullable(label)
        .orElseGet(() -> createSimpleLabel(balanceSheet, name));
  }

  private Label createSimpleLabel(BalanceSheet balanceSheet, String name) {
    Label label = new Label(UUID.randomUUID(), balanceSheet, name);
    labelRepo.save(label);
    return label;
  }

  List<String> getStringsByLabels(Collection<Label> labels) {
    return labels.stream()
        .map(Label::getName)
        .collect(Collectors.toList());
  }

  List<Label> getSuggestLabels(UUID bsId, MoneyOperDto moneyOper) {
    // Найдем 10 наиболее часто используемых меток-категорий за последние 30 дней.
    LocalDate startDate = LocalDate.now().minusDays(30);
    BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
    return moneyOperRepo.findByBalanceSheetAndStatusAndPerformedGreaterThan(balanceSheet, MoneyOperStatus.done, startDate)
      .flatMap(oper -> oper.getLabels().stream())
      .filter(label -> !moneyOper.getLabels().contains(label))
      .collect(Collectors.groupingBy(label -> label, Collectors.counting()))
      .entrySet()
      .stream()
      .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());
  }

  public MoneyOperDto moneyOperToDto(MoneyOper moneyOper) {
    MoneyOperDto moneyOperDto = new MoneyOperDto(moneyOper.getId(), moneyOper.getStatus(), moneyOper.getPerformed(), moneyOper.getFromAccId(),
        moneyOper.getToAccId(), moneyOper.getAmount().abs(), moneyOper.getCurrencyCode(),
        moneyOper.getToAmount(), moneyOper.getToCurrencyCode(), moneyOper.getPeriod(), moneyOper.getComment(),
        getStringsByLabels(moneyOper.getLabels()), moneyOper.getDateNum(), moneyOper.getParentOperId(),
        moneyOper.getRecurrenceId(), moneyOper.getCreated());
    moneyOperDto.setFromAccName(getAccountName(moneyOper.getFromAccId()));
    moneyOperDto.setToAccName(getAccountName(moneyOper.getToAccId()));
    moneyOperDto.setType(moneyOper.getType().name());
    moneyOperDto.setItems(moneyOper.getItems());
    return moneyOperDto;
  }

  public String getAccountName(UUID accountId) {
    Account account = accountRepo.findOne(accountId);
    return account.getName();
  }

  Stream<Label> getLabels(UUID bsId) {
    BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
    return labelRepo.findByBalanceSheetOrderByName(balanceSheet);
  }
}
