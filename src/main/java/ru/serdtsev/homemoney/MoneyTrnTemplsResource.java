package ru.serdtsev.homemoney;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.serdtsev.homemoney.account.AccountRepository;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.common.HmException;
import ru.serdtsev.homemoney.common.HmResponse;
import ru.serdtsev.homemoney.dao.MoneyTrnsDao;
import ru.serdtsev.homemoney.dto.MoneyTrn;
import ru.serdtsev.homemoney.dto.MoneyTrnTempl;
import ru.serdtsev.homemoney.moneyoper.Label;
import ru.serdtsev.homemoney.moneyoper.MoneyOper;
import ru.serdtsev.homemoney.moneyoper.MoneyOperService;

import javax.transaction.Transactional;
import java.sql.Date;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/{bsId}/money-trn-templs")
public class MoneyTrnTemplsResource {
  private final MoneyTrnsDao moneyTrnsDao;
  private final MoneyOperService moneyOperService;
  private final BalanceSheetRepository balanceSheetRepo;
  private final AccountRepository accountRepo;

  @Autowired
  public MoneyTrnTemplsResource(MoneyTrnsDao moneyTrnsDao, MoneyOperService moneyOperService,
      BalanceSheetRepository balanceSheetRepo, AccountRepository accountRepo) {
    this.moneyTrnsDao = moneyTrnsDao;
    this.moneyOperService = moneyOperService;
    this.balanceSheetRepo = balanceSheetRepo;
    this.accountRepo = accountRepo;
  }

  @RequestMapping
  @Transactional
  public HmResponse getList(
      @PathVariable UUID bsId,
      @RequestParam(required = false, defaultValue = "") String search) {
    try {
      BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
      List<MoneyTrnTempl> list = moneyOperService.getRecurrenceOpers(balanceSheet, search)
          .map(this::moneyOperToMoneyTrnTempl)
          .collect(Collectors.toList());
      return HmResponse.getOk(list);
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  private MoneyTrnTempl moneyOperToMoneyTrnTempl(MoneyOper oper) {
    String fromAccName = accountRepo.findOne(oper.getFromAccId()).getName();
    String toAccName = accountRepo.findOne(oper.getToAccId()).getName();
    return new MoneyTrnTempl(oper.getId(), oper.getTemplateId(), oper.getTemplateId(),
        oper.getNextDate(), oper.getPeriod(), oper.getFromAccId(), oper.getToAccId(), oper.getAmount(), oper.getComment(),
        getStringsByLabels(oper.getLabels()), oper.getCurrencyCode(), oper.getToCurrencyCode(), fromAccName, toAccName);
  }

  private List<String> getStringsByLabels(Collection<Label> labels) {
    return labels.stream()
        .map(Label::getName)
        .collect(Collectors.toList());
  }

  @RequestMapping("/create")
  public HmResponse create(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrn moneyTrn) {
    Date nextDate = MoneyTrnTempl.calcNextDate(moneyTrn.getTrnDate(), moneyTrn.getPeriod());
    MoneyTrnTempl templ = new MoneyTrnTempl(UUID.randomUUID(), moneyTrn.getId(), moneyTrn.getId(), nextDate,
        moneyTrn.getPeriod(), moneyTrn.getFromAccId(), moneyTrn.getToAccId(), moneyTrn.getAmount(),
        moneyTrn.getComment(), moneyTrn.getLabels(), moneyTrn.getCurrencyCode(), moneyTrn.getToCurrencyCode(),
        moneyTrn.getFromAccName(), moneyTrn.getToAccName());
    moneyTrnsDao.createMoneyTrnTempl(bsId, templ);
    return HmResponse.getOk();
  }

  @RequestMapping("/skip")
  public HmResponse skip(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrnTempl templ) {
    templ.setNextDate(MoneyTrnTempl.calcNextDate(templ.getNextDate(), templ.getPeriod()));
    moneyTrnsDao.updateMoneyTrnTempl(bsId, templ);
    return HmResponse.getOk();
  }

  @RequestMapping("/delete")
  public HmResponse delete(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrnTempl templ) {
    try {
      moneyTrnsDao.deleteMoneyTrnTempl(bsId, templ.getId());
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/update")
  public HmResponse updateTempl(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrnTempl templ) {
    try {
      moneyTrnsDao.updateMoneyTrnTempl(bsId, templ);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

}
