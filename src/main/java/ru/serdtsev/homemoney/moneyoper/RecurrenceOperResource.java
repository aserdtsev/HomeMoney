package ru.serdtsev.homemoney.moneyoper;

import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.serdtsev.homemoney.account.AccountRepository;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.common.HmException;
import ru.serdtsev.homemoney.common.HmResponse;
import ru.serdtsev.homemoney.moneyoper.model.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/{bsId}/recurrence-opers")
@Transactional
public class RecurrenceOperResource {
  private final MoneyOperService moneyOperService;
  private final BalanceSheetRepository balanceSheetRepo;
  private final AccountRepository accountRepo;
  private final ConversionService conversionService;

  @Autowired
  public RecurrenceOperResource(MoneyOperService moneyOperService, BalanceSheetRepository balanceSheetRepo,
       AccountRepository accountRepo, @Qualifier("conversionService") ConversionService conversionService) {
    this.moneyOperService = moneyOperService;
    this.balanceSheetRepo = balanceSheetRepo;
    this.accountRepo = accountRepo;
    this.conversionService = conversionService;
  }

  @RequestMapping
  @Transactional(readOnly = true)
  public HmResponse getList(
      @PathVariable UUID bsId,
      @RequestParam(required = false, defaultValue = "") String search) {
    try {
      BalanceSheet balanceSheet = balanceSheetRepo.findById(bsId).get();
      List<RecurrenceOperDto> list = moneyOperService.getRecurrenceOpers(balanceSheet, search)
          .sorted(Comparator.comparing(RecurrenceOper::getNextDate))
          .map(this::recurrenceOperToDto)
          .collect(Collectors.toList());
      return HmResponse.getOk(list);
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  private RecurrenceOperDto recurrenceOperToDto(RecurrenceOper recurrenceOper) {
    MoneyOper oper = recurrenceOper.getTemplate();
    String fromAccName = accountRepo.findById(oper.getFromAccId()).get().getName();
    String toAccName = accountRepo.findById(oper.getToAccId()).get().getName();
    RecurrenceOperDto dto = new RecurrenceOperDto(recurrenceOper.getId(), oper.getId(), oper.getId(),
            recurrenceOper.getNextDate(), oper.getPeriod(), oper.getFromAccId(), oper.getToAccId(), oper.getAmount(), oper.getToAmount(),
            oper.getComment(), getStringsByLabels(oper.getLabels()), oper.getCurrencyCode(), oper.getToCurrencyCode(), fromAccName,
            toAccName, oper.getType().name());
    val items = oper.getItems().stream()
            .map(item -> conversionService.convert(item, MoneyOperItemDto.class))
            .sorted(Comparator.comparing(MoneyOperItemDto::getValue))
            .collect(Collectors.toList());
    dto.setItems(items);
    return dto;
  }

  private List<String> getStringsByLabels(Collection<Label> labels) {
    return labels.stream()
        .map(Label::getName)
        .collect(Collectors.toList());
  }

  @RequestMapping("/create")
  public HmResponse create(
      @PathVariable UUID bsId,
      @RequestBody MoneyOperDto moneyOperDto) {
    BalanceSheet balanceSheet = balanceSheetRepo.findById(bsId).get();
    moneyOperService.createRecurrenceOper(balanceSheet, moneyOperDto.getId());
    return HmResponse.getOk();
  }

  @RequestMapping("/skip")
  public HmResponse skip(
      @PathVariable UUID bsId,
      @RequestBody RecurrenceOperDto oper) {
    BalanceSheet balanceSheet = balanceSheetRepo.findById(bsId).get();
    moneyOperService.skipRecurrenceOper(balanceSheet, oper.getId());
    return HmResponse.getOk();
  }

  @RequestMapping("/delete")
  public HmResponse delete(
      @PathVariable UUID bsId,
      @RequestBody RecurrenceOperDto oper) {
    try {
      BalanceSheet balanceSheet = balanceSheetRepo.findById(bsId).get();
      moneyOperService.deleteRecurrenceOper(balanceSheet, oper.getId());
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/update")
  public HmResponse updateRecurrenceOper(
      @PathVariable UUID bsId,
      @RequestBody RecurrenceOperDto oper) {
    try {
      BalanceSheet balanceSheet = balanceSheetRepo.findById(bsId).get();
      moneyOperService.updateRecurrenceOper(balanceSheet, oper);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

}
