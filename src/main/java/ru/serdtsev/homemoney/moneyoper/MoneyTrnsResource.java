package ru.serdtsev.homemoney.moneyoper;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.serdtsev.homemoney.account.*;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.common.HmException;
import ru.serdtsev.homemoney.common.HmResponse;
import ru.serdtsev.homemoney.dto.MoneyTrn;
import ru.serdtsev.homemoney.dto.PagedList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.*;
import static ru.serdtsev.homemoney.common.HmResponse.getFail;
import static ru.serdtsev.homemoney.common.HmResponse.getOk;
import static ru.serdtsev.homemoney.moneyoper.MoneyOperStatus.*;
import static ru.serdtsev.homemoney.moneyoper.MoneyOperType.expense;
import static ru.serdtsev.homemoney.moneyoper.MoneyOperType.income;
import static ru.serdtsev.homemoney.utils.Utils.nvl;

@RestController
@RequestMapping("/api/{bsId}/money-trns")
public class MoneyTrnsResource {
  private static final String SEARCH_DATE_REGEX = "\\p{Digit}{4}-\\p{Digit}{2}-\\p{Digit}{2}";
  private static final String SEARCH_UUID_REGEX = "\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}";
  private static final String SEARCH_MONEY_REGEX = "\\p{Digit}+\\.*\\p{Digit}*";
  private final MoneyOperService moneyOperService;
  private final BalanceSheetRepository balanceSheetRepo;
  private final AccountRepository accountRepo;
  private final MoneyOperRepository moneyOperRepo;
  private final LabelRepository labelRepo;
  private final MoneyOperItemRepo moneyOperItemRepo;
  private final CategoryRepository categoryRepo;

  @Autowired
  public MoneyTrnsResource(MoneyOperService moneyOperService, BalanceSheetRepository balanceSheetRepo,
      AccountRepository accountRepo, MoneyOperRepository moneyOperRepo, LabelRepository labelRepo,
      MoneyOperItemRepo moneyOperItemRepo, CategoryRepository categoryRepo) {
    this.moneyOperService = moneyOperService;
    this.balanceSheetRepo = balanceSheetRepo;
    this.accountRepo = accountRepo;
    this.moneyOperRepo = moneyOperRepo;
    this.labelRepo = labelRepo;
    this.moneyOperItemRepo = moneyOperItemRepo;
    this.categoryRepo = categoryRepo;
  }

  @RequestMapping
  @Transactional(readOnly = true)
  public HmResponse getMoneyTrns(
      @PathVariable UUID bsId,
      @RequestParam(required = false, defaultValue = "") String search,
      @RequestParam(required = false, defaultValue = "10") int limit,
      @RequestParam(required = false, defaultValue = "0") int offset) {
    try {
      ArrayList<MoneyTrn> trns = new ArrayList<>();
      BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
      if (offset == 0) {
        List<MoneyTrn> pendingTrns = getMoneyOpers(balanceSheet, MoneyOperStatus.pending, search, limit + 1, offset)
            .stream()
            .map(this::moneyOperToMoneyTrn)
            .collect(Collectors.toList());
        trns.addAll(pendingTrns);

        LocalDate beforeDate = LocalDate.now().plusDays(14L);
        List<MoneyTrn> recurrenceTrns = moneyOperService.getNextRecurrenceOpers(balanceSheet, search, Date.valueOf(beforeDate))
            .map(this::moneyOperToMoneyTrn)
            .collect(Collectors.toList());
        trns.addAll(recurrenceTrns);

        trns.sort(Comparator.comparing(MoneyTrn::getTrnDate).reversed());
      }

      List<MoneyTrn> doneTrns = getMoneyOpers(balanceSheet, MoneyOperStatus.done, search, limit + 1, offset)
          .stream()
          .map(this::moneyOperToMoneyTrn)
          .collect(Collectors.toList());
      boolean hasNext = doneTrns.size() > limit;
      trns.addAll(hasNext ? doneTrns.subList(0, limit) : doneTrns);
      PagedList<MoneyTrn> pagedList = new PagedList<>(trns, limit, offset, hasNext);
      return getOk(pagedList);
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }

  private MoneyTrn moneyOperToMoneyTrn(MoneyOper moneyOper) {
    MoneyTrn moneyTrn = new MoneyTrn(moneyOper.getId(), moneyOper.getStatus(), moneyOper.getPerformed(), moneyOper.getFromAccId(),
        moneyOper.getToAccId(), moneyOper.getAmount().abs(), moneyOper.getCurrencyCode(),
        moneyOper.getToAmount(), moneyOper.getToCurrencyCode(), moneyOper.getPeriod(), moneyOper.getComment(),
        moneyOperService.getStringsByLabels(moneyOper.getLabels()), moneyOper.getDateNum(), moneyOper.getParentOperId(),
        moneyOper.getRecurrenceId(), moneyOper.getCreated());
    moneyTrn.setFromAccName(getAccountName(moneyOper.getFromAccId()));
    moneyTrn.setToAccName(getAccountName(moneyOper.getToAccId()));
    moneyTrn.setType(moneyOper.getType().name());
    moneyTrn.setItems(moneyOper.getItems());
    return moneyTrn;
  }


  private List<MoneyOper> getMoneyOpers(BalanceSheet balanceSheet, MoneyOperStatus status, @Nullable String search,
      Integer limit, Integer offset) {
    Sort sort = new Sort(Sort.Direction.DESC, "performed")
        .and(new Sort("dateNum"))
        .and(new Sort(Sort.Direction.DESC, "created"));
    return Strings.isBlank(search)
        ? getMoneyOpers(balanceSheet, status, sort, limit, offset)
        : getMoneyOpersBySearch(balanceSheet, status, search.toLowerCase(), sort, limit, offset);
  }

  private List<MoneyOper> getMoneyOpers(BalanceSheet balanceSheet, MoneyOperStatus status, Sort sort, Integer limit, Integer offset) {
    Pageable pageRequest = new PageRequest(offset/(limit-1), limit-1, sort);
    List<MoneyOper> opers = new ArrayList<>();
    opers.addAll(moneyOperRepo.findByBalanceSheetAndStatus(balanceSheet, status, pageRequest).getContent());
    opers.addAll(moneyOperRepo.findByBalanceSheetAndStatus(balanceSheet, status, pageRequest.next()).getContent().stream()
        .limit(1)
        .collect(Collectors.toList()));
    return opers;
  }

  private List<MoneyOper> getMoneyOpersBySearch(BalanceSheet balanceSheet, MoneyOperStatus status, String search,
      Sort sort, Integer limit, Integer offset) {
    Pageable pageRequest = new PageRequest(0, 100, sort);
    List<MoneyOper> opers = new ArrayList<>();
    Page page;
    final Function<Pageable, Page> pager;
    final Consumer<Page> adder;

    if (search.matches(SEARCH_DATE_REGEX)) {
      // по дате в формате ISO
      pager = pageable -> moneyOperRepo.findByBalanceSheetAndStatusAndPerformed(balanceSheet, status, Date.valueOf(search), pageable);
      //noinspection unchecked
      adder = p -> opers.addAll(p.getContent());
    } else if (search.matches(SEARCH_UUID_REGEX)) {
      // по идентификатору операции
      pager = pageable -> moneyOperRepo.findByBalanceSheetAndStatusAndId(balanceSheet, status, UUID.fromString(search), pageable);
      //noinspection unchecked
      adder = p -> opers.addAll(p.getContent());
    } else if (search.matches(SEARCH_MONEY_REGEX)) {
      // по сумме операции
      pageRequest = new PageRequest(pageRequest.getPageNumber(), pageRequest.getPageSize());
      pager = pageable -> {
        BigDecimal value = new BigDecimal(search).abs();
        return moneyOperItemRepo.findByBalanceSheetAndValueOrderByPerformedDesc(balanceSheet, value, pageable);
      };
      //noinspection unchecked
      adder = p -> ((Page<MoneyOperItem>) p).getContent()
          .stream()
          .map(MoneyOperItem::getMoneyOper)
          .filter(oper -> oper.getStatus() != status)
          .distinct()
          .forEach(opers::add);
    } else {
      pager = pageable -> moneyOperRepo.findByBalanceSheetAndStatus(balanceSheet, status, pageable);
      adder = p -> {
        @SuppressWarnings("unchecked") List<MoneyOper> opersChunk = ((Page<MoneyOper>) p).getContent().stream()
            .filter(oper ->
                // по имени счета
                oper.getItems().stream().anyMatch(item -> item.getBalance().getName().toLowerCase().contains(search))
                // по комментарию
                || oper.getComment().toLowerCase().contains(search)
                // по меткам
                || oper.getLabels().stream().anyMatch(label -> label.getName().toLowerCase().contains(search)))
                .collect(Collectors.toList());
        opers.addAll(opersChunk);
      };
    }

    do {
      page = pager.apply(pageRequest);
      adder.accept(page);
      pageRequest = pageRequest.next();
    } while (opers.size()-offset < limit && page.hasNext());
    return opers.subList(offset, opers.size()).stream().limit(limit).collect(Collectors.toList());
  }

  @RequestMapping("/item")
  @Transactional(readOnly = true)
  public HmResponse getMoneyTrn(
      @PathVariable UUID bsId,
      @RequestParam UUID id) {
    try {
      MoneyOper oper = moneyOperRepo.findOne(id);
      moneyOperService.checkMoneyOperBelongsBalanceSheet(oper, bsId);
      return getOk(moneyOperToMoneyTrn(oper));
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }

  @RequestMapping("/create")
  @Transactional
  public HmResponse createMoneyTrn(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrn moneyTrn) {
    List<MoneyTrn> moneyTrns = createMoneyOperInternal(bsId, moneyTrn)
        .map(this::moneyOperToMoneyTrn)
        .collect(Collectors.toList());
    return getOk(moneyTrns);
  }

  @RequestMapping("/update")
  @Transactional
  public HmResponse updateMoneyTrn(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrn moneyTrn) {
    try {
      MoneyOper origOper = moneyOperRepo.findOne(moneyTrn.getId());
      if (isNull(origOper)) {
        createMoneyOperInternal(bsId, moneyTrn);
        return getOk();
      }

      moneyOperService.checkMoneyOperBelongsBalanceSheet(origOper, bsId);

      BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
      MoneyOper oper = newMoneyOper(balanceSheet, moneyTrn);

      boolean essentialEquals = origOper.essentialEquals(oper);
      MoneyOperStatus origPrevStatus = origOper.getStatus();
      if (!essentialEquals && origOper.getStatus() == done) {
        origOper.cancel();
      }

      origOper.setPerformed(oper.getPerformed());
      origOper.setLabels(oper.getLabels());
      origOper.setDateNum(oper.getDateNum());
      origOper.setPeriod(oper.getPeriod());
      origOper.setComment(oper.getComment());
      moneyOperService.updateFromAccount(origOper, oper.getFromAccId());
      moneyOperService.updateToAccount(origOper, oper.getToAccId());
      moneyOperService.updateAmount(origOper, oper.getAmount());
      moneyOperService.updateToAmount(origOper, oper.getToAmount());

      if (!essentialEquals && origPrevStatus == done || origOper.getStatus() == pending && moneyTrn.getStatus() == done) {
        origOper.complete();
      }

      moneyOperRepo.save(origOper);
      return getOk();
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }

  @RequestMapping("/delete")
  @Transactional
  public HmResponse deleteMoneyTrn(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrn moneyTrn) {
    try {
      MoneyOper oper = moneyOperRepo.findOne(moneyTrn.getId());
      requireNonNull(oper);
      moneyOperService.checkMoneyOperBelongsBalanceSheet(oper, bsId);
      oper.cancel();
      moneyOperRepo.save(oper);
      return getOk();
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }

  @RequestMapping("/skip")
  @Transactional
  public HmResponse skipMoneyTrn(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrn moneyTrn) throws SQLException {
    try {
      MoneyOperStatus status = moneyTrn.getStatus();
      if (status == MoneyOperStatus.pending) {
        skipPendingMoneyTrn(bsId, moneyTrn);
      } else if (status == recurrence) {
        skipRecurrenceMoneyTrn(bsId, moneyTrn);
      }
      return getOk();
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }

  private void skipPendingMoneyTrn(UUID bsId, MoneyTrn moneyTrn) {
    MoneyOper oper = moneyOperRepo.findOne(moneyTrn.getId());
    requireNonNull(oper);
    moneyOperService.checkMoneyOperBelongsBalanceSheet(oper, bsId);
    oper.cancel();
    oper.setRecurrenceId(null);
    moneyOperRepo.save(oper);
  }

  private void skipRecurrenceMoneyTrn(UUID bsId, MoneyTrn moneyTrn) {
    moneyOperService.findRecurrenceOper(moneyTrn.getTemplId()).ifPresent(recurrenceOper -> {
      recurrenceOper.skipNextDate();
      moneyOperService.save(recurrenceOper);
    });
  }

  @RequestMapping("/up")
  @Transactional
  public HmResponse upMoneyTrn(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrn moneyTrn) {
    try {
      MoneyOper oper = moneyOperRepo.findOne(moneyTrn.getId());
      List<MoneyOper> opers = moneyOperRepo.findByBalanceSheetAndStatusAndPerformed(oper.getBalanceSheet(),
          MoneyOperStatus.done, oper.getPerformed())
          .sorted(Comparator.comparing(MoneyOper::getDateNum))
          .collect(Collectors.toList());
      int index = opers.indexOf(oper);
      if (index > 0) {
        MoneyOper prevOper = opers.get(index - 1);
        opers.set(index - 1, oper);
        opers.set(index, prevOper);
        IntStream.range(0, opers.size()).forEach(i -> {
          MoneyOper o = opers.get(i);
          o.setDateNum(i);
          moneyOperRepo.save(o);
        });
      }
      return getOk();
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }

  private Stream<MoneyOper> createMoneyOperInternal(UUID bsId, MoneyTrn moneyTrn) {
    BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
    List<MoneyOper> moneyOpers = new ArrayList<>();

    MoneyOper mainOper = newMainMoneyOper(balanceSheet, moneyTrn);
    moneyOperRepo.save(mainOper);
    moneyOpers.add(mainOper);

    newReserveMoneyOper(balanceSheet, moneyTrn).ifPresent(moneyOpers::add);

    if (nonNull(mainOper.getRecurrenceId())) {
      moneyOperService.skipRecurrenceOper(balanceSheet, mainOper.getRecurrenceId());
    }

    if ((moneyTrn.getStatus() == done || moneyTrn.getStatus() == doneNew) && !mainOper.getPerformed().toLocalDate().isAfter(LocalDate.now())) {
      moneyOpers.forEach(MoneyOper::complete);
    }
    moneyOpers.forEach(moneyOperService::save);

    return moneyOpers.stream();
  }

  private String getAccountName(UUID accountId) {
    Account account = accountRepo.findOne(accountId);
    return account.getName();
  }

  @Nonnull
  private MoneyOper newMainMoneyOper(BalanceSheet balanceSheet, MoneyTrn moneyTrn) {
    return newMoneyOper(balanceSheet, moneyTrn);
  }

  private Optional<MoneyOper> newReserveMoneyOper(BalanceSheet balanceSheet, MoneyTrn moneyTrn) {
    Account fromAcc = balanceSheet.getSvcRsv();
    Account account = accountRepo.findOne(moneyTrn.getFromAccId());
    if (account.getType() == AccountType.debit) {
      Balance balance = (Balance) account;
      if (balance.getReserve() != null) {
        fromAcc = balance.getReserve();
      }
    }

    Account toAcc = balanceSheet.getSvcRsv();
    account = accountRepo.findOne(moneyTrn.getToAccId());
    if (account.getType() == AccountType.debit) {
      Balance balance = (Balance) account;
      if (balance.getReserve() != null) {
        toAcc = balance.getReserve();
      }
    }

    MoneyOper reserveMoneyOper = null;
    if (!Objects.equals(fromAcc, toAcc)) {
      List<Label> labels = moneyOperService.getLabelsByStrings(balanceSheet, moneyTrn.getLabels());
      reserveMoneyOper = moneyOperService.newMoneyOper(balanceSheet, UUID.randomUUID(), MoneyOperStatus.pending, moneyTrn.getTrnDate(),
          nvl(moneyTrn.getDateNum(), 0), labels, moneyTrn.getComment(), moneyTrn.getPeriod(), fromAcc.getId(), toAcc.getId(),
          moneyTrn.getAmount(), moneyTrn.getAmount(), moneyTrn.getId(), null);
    }
    return Optional.ofNullable(reserveMoneyOper);
  }

  @Nonnull
  private MoneyOper newMoneyOper(BalanceSheet balanceSheet, MoneyTrn moneyTrn) {
    List<Label> labels = moneyOperService.getLabelsByStrings(balanceSheet, moneyTrn.getLabels());
    categoryToLabel(balanceSheet, moneyTrn).ifPresent(labels::add);
    BigDecimal amount = moneyTrn.getAmount();
    if (isNull(amount) && nonNull(moneyTrn.getToAmount())) {
      amount = moneyTrn.getToAmount();
    }
    assert nonNull(amount);
    MoneyOper templateOper = nonNull(moneyTrn.getTemplId()) ? moneyOperRepo.findOne(moneyTrn.getTemplId()) : null;
    return moneyOperService.newMoneyOper(balanceSheet, moneyTrn.getId(), pending, moneyTrn.getTrnDate(), nvl(moneyTrn.getDateNum(), 0),
        labels, moneyTrn.getComment(), moneyTrn.getPeriod(), moneyTrn.getFromAccId(), moneyTrn.getToAccId(),
        amount, nvl(moneyTrn.getToAmount(), amount), null, templateOper);
  }

  private Optional<Label> categoryToLabel(BalanceSheet balanceSheet, MoneyTrn trn) {
    Category category = null;
    MoneyOperType operType = MoneyOperType.valueOf(trn.getType());
    if (operType.equals(expense)) {
      category = categoryRepo.findOne(trn.getToAccId());
    } else if (operType.equals(income)) {
      category = categoryRepo.findOne(trn.getFromAccId());
    }
    if (isNull(category)) {
      return Optional.empty();
    }
    Label label = labelRepo.findByBalanceSheetAndName(balanceSheet, category.getName());
    if (isNull(label)) {
      Category rootCategory = category.getRoot();
      Label rootLabel = null;
      if (nonNull(rootCategory)) {
        rootLabel = labelRepo.findByBalanceSheetAndName(balanceSheet, rootCategory.getName());
        if (isNull(rootLabel)) {
          rootLabel = new Label(UUID.randomUUID(), balanceSheet, rootCategory.getName(), null, true);
          labelRepo.save(rootLabel);
        }
      }
      UUID rootLabelId = nonNull(rootLabel) ? rootLabel.getId() : null;
      label = new Label(UUID.randomUUID(), balanceSheet, category.getName(), rootLabelId, true);
      labelRepo.save(label);
    }
    return Optional.of(label);
  }

}
