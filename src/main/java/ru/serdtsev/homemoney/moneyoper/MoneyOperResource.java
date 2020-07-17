package ru.serdtsev.homemoney.moneyoper;

import lombok.val;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.serdtsev.homemoney.account.AccountRepository;
import ru.serdtsev.homemoney.account.BalanceRepository;
import ru.serdtsev.homemoney.account.CategoryRepository;
import ru.serdtsev.homemoney.account.model.*;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.common.HmException;
import ru.serdtsev.homemoney.common.HmResponse;
import ru.serdtsev.homemoney.dto.PagedList;
import ru.serdtsev.homemoney.moneyoper.model.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
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
import static ru.serdtsev.homemoney.moneyoper.model.MoneyOperStatus.*;
import static ru.serdtsev.homemoney.moneyoper.model.MoneyOperType.expense;
import static ru.serdtsev.homemoney.moneyoper.model.MoneyOperType.income;
import static ru.serdtsev.homemoney.utils.Utils.nvl;

@RestController
@RequestMapping("/api/{bsId}/money-opers")
@Transactional
public class MoneyOperResource {
  private static final String SEARCH_DATE_REGEX = "\\p{Digit}{4}-\\p{Digit}{2}-\\p{Digit}{2}";
  private static final String SEARCH_UUID_REGEX = "\\p{Alnum}{8}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{4}-\\p{Alnum}{12}";
  private static final String SEARCH_MONEY_REGEX = "\\p{Digit}+\\.*\\p{Digit}*";
  private final MoneyOperService moneyOperService;
  private final BalanceSheetRepository balanceSheetRepo;
  private final AccountRepository accountRepo;
  private final BalanceRepository balanceRepo;
  private final MoneyOperRepo moneyOperRepo;
  private final LabelRepository labelRepo;
  private final MoneyOperItemRepo moneyOperItemRepo;
  private final CategoryRepository categoryRepo;

  @Autowired
  public MoneyOperResource(MoneyOperService moneyOperService, BalanceSheetRepository balanceSheetRepo,
      AccountRepository accountRepo, BalanceRepository balanceRepo, MoneyOperRepo moneyOperRepo, LabelRepository labelRepo,
      MoneyOperItemRepo moneyOperItemRepo, CategoryRepository categoryRepo) {
    this.moneyOperService = moneyOperService;
    this.balanceSheetRepo = balanceSheetRepo;
    this.accountRepo = accountRepo;
    this.balanceRepo = balanceRepo;
    this.moneyOperRepo = moneyOperRepo;
    this.labelRepo = labelRepo;
    this.moneyOperItemRepo = moneyOperItemRepo;
    this.categoryRepo = categoryRepo;
  }

  @RequestMapping
  @Transactional(readOnly = true)
  public HmResponse getMoneyOpers(
      @PathVariable UUID bsId,
      @RequestParam(required = false, defaultValue = "") String search,
      @RequestParam(required = false, defaultValue = "10") int limit,
      @RequestParam(required = false, defaultValue = "0") int offset) {
    try {
      ArrayList<MoneyOperDto> opers = new ArrayList<>();
      BalanceSheet balanceSheet = balanceSheetRepo.findById(bsId).get();
      if (offset == 0) {
        List<MoneyOperDto> pendingOpers = getMoneyOpers(balanceSheet, MoneyOperStatus.pending, search, limit + 1, offset)
            .stream()
            .map((MoneyOper moneyOper) -> moneyOperService.moneyOperToDto(moneyOper))
            .collect(Collectors.toList());
        opers.addAll(pendingOpers);

        LocalDate beforeDate = LocalDate.now().plusDays(30);
        List<MoneyOperDto> recurrenceOpers = moneyOperService.getNextRecurrenceOpers(balanceSheet, search, beforeDate)
            .map((MoneyOper moneyOper) -> moneyOperService.moneyOperToDto(moneyOper))
            .collect(Collectors.toList());
        opers.addAll(recurrenceOpers);

        opers.sort(Comparator.comparing(MoneyOperDto::getOperDate).reversed());
      }

      List<MoneyOperDto> doneOpers = getMoneyOpers(balanceSheet, MoneyOperStatus.done, search, limit + 1, offset)
          .stream()
          .map((MoneyOper moneyOper) -> moneyOperService.moneyOperToDto(moneyOper))
          .collect(Collectors.toList());
      boolean hasNext = doneOpers.size() > limit;
      opers.addAll(hasNext ? doneOpers.subList(0, limit) : doneOpers);
      PagedList<MoneyOperDto> pagedList = new PagedList<>(opers, limit, offset, hasNext);
      return getOk(pagedList);
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }

  private List<MoneyOper> getMoneyOpers(BalanceSheet balanceSheet, MoneyOperStatus status, @Nullable String search,
      Integer limit, Integer offset) {
    Sort sort = Sort.by(Sort.Direction.DESC, "performed")
        .and(Sort.by("dateNum"))
        .and(Sort.by(Sort.Direction.DESC, "created"));
    return Strings.isBlank(search)
        ? getMoneyOpers(balanceSheet, status, sort, limit, offset)
        : getMoneyOpersBySearch(balanceSheet, status, search.toLowerCase(), sort, limit, offset);
  }

  private List<MoneyOper> getMoneyOpers(BalanceSheet balanceSheet, MoneyOperStatus status, Sort sort, Integer limit, Integer offset) {
    Pageable pageRequest = PageRequest.of(offset/(limit-1), limit-1, sort);
    List<MoneyOper> opers = new ArrayList<>();
    opers.addAll(moneyOperRepo.findByBalanceSheetAndStatus(balanceSheet, status, pageRequest).getContent());
    opers.addAll(moneyOperRepo.findByBalanceSheetAndStatus(balanceSheet, status, pageRequest.next()).getContent().stream()
        .limit(1)
        .collect(Collectors.toList()));
    return opers;
  }

  private List<MoneyOper> getMoneyOpersBySearch(BalanceSheet balanceSheet, MoneyOperStatus status, String search,
      Sort sort, Integer limit, Integer offset) {
    Pageable pageRequest = PageRequest.of(0, 100, sort);
    List<MoneyOper> opers = new ArrayList<>();
    Page page;
    final Function<Pageable, Page> pager;
    final Consumer<Page> adder;

    if (search.matches(SEARCH_DATE_REGEX)) {
      // по дате в формате ISO
      pager = pageable -> moneyOperRepo.findByBalanceSheetAndStatusAndPerformed(balanceSheet, status, LocalDate.parse(search), pageable);
      //noinspection unchecked
      adder = p -> opers.addAll(p.getContent());
    } else if (search.matches(SEARCH_UUID_REGEX)) {
      // по идентификатору операции
      pager = pageable -> moneyOperRepo.findByBalanceSheetAndStatusAndId(balanceSheet, status, UUID.fromString(search), pageable);
      //noinspection unchecked
      adder = p -> opers.addAll(p.getContent());
    } else if (search.matches(SEARCH_MONEY_REGEX)) {
      // по сумме операции
      pageRequest = PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize());
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
  public HmResponse getMoneyOpers(
      @PathVariable UUID bsId,
      @RequestParam UUID id) {
    try {
      MoneyOper oper = moneyOperRepo.findById(id).get();
      moneyOperService.checkMoneyOperBelongsBalanceSheet(oper, bsId);
      return getOk(moneyOperService.moneyOperToDto(oper));
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }

  @RequestMapping("/create")
  public HmResponse createMoneyOper(
      @PathVariable UUID bsId,
      @RequestBody MoneyOperDto moneyOperDto) {
    List<MoneyOperDto> moneyOperDtos = createMoneyOperInternal(bsId, moneyOperDto)
        .map((MoneyOper moneyOper) -> moneyOperService.moneyOperToDto(moneyOper))
        .collect(Collectors.toList());
    return getOk(moneyOperDtos);
  }

  @RequestMapping("/update")
  public HmResponse updateMoneyOper(
      @PathVariable UUID bsId,
      @RequestBody MoneyOperDto moneyOperDto) {
    try {
      MoneyOper origOper = moneyOperRepo.findById(moneyOperDto.getId()).orElse(null);
      if (isNull(origOper)) {
        createMoneyOperInternal(bsId, moneyOperDto);
        return getOk();
      }

      moneyOperService.checkMoneyOperBelongsBalanceSheet(origOper, bsId);

      BalanceSheet balanceSheet = balanceSheetRepo.findById(bsId).get();
      MoneyOper oper = newMoneyOper(balanceSheet, moneyOperDto);

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

      val toCurrencyCode = nvl(oper.getToCurrencyCode(), oper.getCurrencyCode());
      BigDecimal toAmount = Objects.equals(oper.getCurrencyCode(), toCurrencyCode)
          ? oper.getAmount() 
          : oper.getToAmount();
      moneyOperService.updateToAmount(origOper, toAmount);

      if (!essentialEquals && origPrevStatus == done || origOper.getStatus() == pending && moneyOperDto.getStatus() == done) {
        origOper.complete();
      }

      moneyOperRepo.save(origOper);
      return getOk();
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }

  @RequestMapping("/delete")
  public HmResponse deleteMoneyOper(
      @PathVariable UUID bsId,
      @RequestBody MoneyOperDto moneyOperDto) {
    try {
      MoneyOper oper = moneyOperRepo.findById(moneyOperDto.getId()).get();
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
  public HmResponse skipMoneyOper(
      @PathVariable UUID bsId,
      @RequestBody MoneyOperDto moneyOperDto) throws SQLException {
    try {
      MoneyOperStatus status = moneyOperDto.getStatus();
      if (status == MoneyOperStatus.pending) {
        skipPendingMoneyOper(bsId, moneyOperDto);
      } else if (status == recurrence) {
        skipRecurrenceMoneyOper(bsId, moneyOperDto);
      }
      return getOk();
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }

  private void skipPendingMoneyOper(UUID bsId, MoneyOperDto moneyOperDto) {
    MoneyOper oper = moneyOperRepo.findById(moneyOperDto.getId()).get();
    requireNonNull(oper);
    moneyOperService.checkMoneyOperBelongsBalanceSheet(oper, bsId);
    oper.cancel();
    oper.setRecurrenceId(null);
    moneyOperRepo.save(oper);
  }

  private void skipRecurrenceMoneyOper(UUID bsId, MoneyOperDto moneyOperDto) {
    moneyOperService.findRecurrenceOper(moneyOperDto.getRecurrenceId()).ifPresent(recurrenceOper -> {
      recurrenceOper.skipNextDate();
      moneyOperService.save(recurrenceOper);
    });
  }

  @RequestMapping("/up")
  public HmResponse upMoneyOper(
      @PathVariable UUID bsId,
      @RequestBody MoneyOperDto moneyOperDto) {
    try {
      MoneyOper oper = moneyOperRepo.findById(moneyOperDto.getId()).get();
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

  private Stream<MoneyOper> createMoneyOperInternal(UUID bsId, MoneyOperDto moneyOperDto) {
    BalanceSheet balanceSheet = balanceSheetRepo.findById(bsId).get();
    List<MoneyOper> moneyOpers = new ArrayList<>();

    MoneyOper mainOper = newMainMoneyOper(balanceSheet, moneyOperDto);
    moneyOperRepo.save(mainOper);
    moneyOpers.add(mainOper);

    newReserveMoneyOper(balanceSheet, moneyOperDto).ifPresent(moneyOpers::add);

    if (nonNull(mainOper.getRecurrenceId())) {
      moneyOperService.skipRecurrenceOper(balanceSheet, mainOper.getRecurrenceId());
    }

    if ((moneyOperDto.getStatus() == done || moneyOperDto.getStatus() == doneNew) && !mainOper.getPerformed().isAfter(LocalDate.now())) {
      moneyOpers.forEach(MoneyOper::complete);
    }
    moneyOpers.forEach(moneyOperService::save);

    return moneyOpers.stream();
  }

  @Nonnull
  private MoneyOper newMainMoneyOper(BalanceSheet balanceSheet, MoneyOperDto moneyOperDto) {
    return newMoneyOper(balanceSheet, moneyOperDto);
  }

  private Optional<MoneyOper> newReserveMoneyOper(BalanceSheet balanceSheet, MoneyOperDto moneyOperDto) {
    Account fromAcc = balanceSheet.getSvcRsv();
    Account account = accountRepo.findById(moneyOperDto.getFromAccId()).get();
    if (account.getType() == AccountType.debit) {
      Balance balance = (Balance) account;
      if (balance.getReserve() != null) {
        fromAcc = balance.getReserve();
      }
    }

    Account toAcc = balanceSheet.getSvcRsv();
    account = accountRepo.findById(moneyOperDto.getToAccId()).get();
    if (account.getType() == AccountType.debit) {
      Balance balance = (Balance) account;
      if (balance.getReserve() != null) {
        toAcc = balance.getReserve();
      }
    }

    MoneyOper reserveMoneyOper = null;
    if (!Objects.equals(fromAcc, toAcc)) {
      List<Label> labels = moneyOperService.getLabelsByStrings(balanceSheet, moneyOperDto.getLabels());
      reserveMoneyOper = moneyOperService.newMoneyOper(balanceSheet, UUID.randomUUID(), MoneyOperStatus.pending, moneyOperDto.getOperDate(),
          nvl(moneyOperDto.getDateNum(), 0), labels, moneyOperDto.getComment(), moneyOperDto.getPeriod(), fromAcc.getId(), toAcc.getId(),
          moneyOperDto.getAmount(), moneyOperDto.getAmount(), moneyOperDto.getId(), null);
    }
    return Optional.ofNullable(reserveMoneyOper);
  }

  @Nonnull
  private MoneyOper newMoneyOper(BalanceSheet balanceSheet, MoneyOperDto moneyOperDto) {
    List<Label> labels = moneyOperService.getLabelsByStrings(balanceSheet, moneyOperDto.getLabels());
    categoryToLabel(balanceSheet, moneyOperDto).ifPresent(labels::add);
    BigDecimal amount = moneyOperDto.getAmount();
    if (isNull(amount) && nonNull(moneyOperDto.getToAmount())) {
      amount = moneyOperDto.getToAmount();
    }
    assert nonNull(amount);
    val fromBalance = balanceRepo.findById(moneyOperDto.getFromAccId()).orElse(null);
    val toBalance = balanceRepo.findById(moneyOperDto.getToAccId()).orElse(null);
    val currencyCode = fromBalance != null ? fromBalance.getCurrencyCode() : toBalance.getCurrencyCode();
    val toCurrencyCode = toBalance != null ? toBalance.getCurrencyCode() : currencyCode;
    val toAmount = Objects.equals(currencyCode, toCurrencyCode) ? amount : moneyOperDto.getToAmount();
    return moneyOperService.newMoneyOper(balanceSheet, moneyOperDto.getId(), pending, moneyOperDto.getOperDate(),
            nvl(moneyOperDto.getDateNum(), 0), labels, moneyOperDto.getComment(), nvl(moneyOperDto.getPeriod(),
            Period.month), moneyOperDto.getFromAccId(), moneyOperDto.getToAccId(), amount, toAmount, null,
            moneyOperDto.getRecurrenceId());
  }

  private Optional<Label> categoryToLabel(BalanceSheet balanceSheet, MoneyOperDto operDto) {
    Category category = null;
    MoneyOperType operType = MoneyOperType.valueOf(operDto.getType());
    if (operType.equals(expense)) {
      category = categoryRepo.findById(operDto.getToAccId()).orElse(null);
    } else if (operType.equals(income)) {
      category = categoryRepo.findById(operDto.getFromAccId()).orElse(null);
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
          val rootCategoryType = CategoryType.valueOf(rootCategory.getType().name());
          rootLabel = new Label(UUID.randomUUID(), balanceSheet, rootCategory.getName(), null, true,
                  rootCategoryType, null);
          labelRepo.save(rootLabel);
        }
      }
      UUID rootLabelId = nonNull(rootLabel) ? rootLabel.getId() : null;
      val categoryType = CategoryType.valueOf(category.getType().name());
      label = new Label(UUID.randomUUID(), balanceSheet, category.getName(), rootLabelId, true, categoryType,
              null);
      labelRepo.save(label);
    }
    return Optional.of(label);
  }

  @RequestMapping(value = "/suggest-labels", method = RequestMethod.POST)
  @Transactional(readOnly = true)
  public HmResponse suggestLabels(
      @PathVariable UUID bsId,
      @RequestBody MoneyOperDto moneyOperDto) {
    List<String> labels = moneyOperService.getSuggestLabels(bsId, moneyOperDto).stream()
        .map(Label::getName)
        .collect(Collectors.toList());
    return getOk(labels);
  }

  @RequestMapping(value = "/labels")
  @Transactional(readOnly = true)
  public HmResponse labels(
      @PathVariable UUID bsId) {
    List<String> labels = moneyOperService.getLabels(bsId)
        .map(Label::getName)
        .collect(Collectors.toList());
    return getOk(labels);
  }
}
