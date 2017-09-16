package ru.serdtsev.homemoney;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.serdtsev.homemoney.common.HmException;
import ru.serdtsev.homemoney.common.HmResponse;
import ru.serdtsev.homemoney.dao.MoneyTrnsDao;
import ru.serdtsev.homemoney.dto.MoneyTrn;
import ru.serdtsev.homemoney.dto.MoneyTrnTempl;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/{bsId}/money-trn-templs")
public class MoneyTrnTemplsResource {
  private MoneyTrnsDao moneyTrnsDao;

  @Autowired
  public MoneyTrnTemplsResource(MoneyTrnsDao moneyTrnsDao) {
    this.moneyTrnsDao = moneyTrnsDao;
  }

  @RequestMapping
  public HmResponse getList(
      @PathVariable UUID bsId,
      @RequestParam(required = false, defaultValue = "") String search) {
    try {
      List<MoneyTrnTempl> list = moneyTrnsDao.getMoneyTrnTempls(bsId, search);
      return HmResponse.getOk(list);
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/create")
  public HmResponse create(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrn moneyTrn) {
    Date nextDate = MoneyTrnTempl.calcNextDate(moneyTrn.getTrnDate(), moneyTrn.getPeriod());
    MoneyTrnTempl templ = new MoneyTrnTempl(UUID.randomUUID(), moneyTrn.getId(), moneyTrn.getId(), nextDate,
        moneyTrn.getPeriod(), moneyTrn.getFromAccId(), moneyTrn.getToAccId(), moneyTrn.getAmount(),
        moneyTrn.getComment(), moneyTrn.getLabels());
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
