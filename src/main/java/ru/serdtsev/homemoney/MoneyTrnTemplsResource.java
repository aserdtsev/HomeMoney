package ru.serdtsev.homemoney;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.serdtsev.homemoney.dao.MoneyTrnTemplsDao;
import ru.serdtsev.homemoney.dto.HmResponse;
import ru.serdtsev.homemoney.dto.MoneyTrn;
import ru.serdtsev.homemoney.dto.MoneyTrnTempl;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/{bsId}/money-trn-templs")
public class MoneyTrnTemplsResource {
  private MoneyTrnTemplsDao moneyTrnTemplsDao;

  @Autowired
  public MoneyTrnTemplsResource(MoneyTrnTemplsDao moneyTrnTemplsDao) {
    this.moneyTrnTemplsDao = moneyTrnTemplsDao;
  }

  @RequestMapping
  public HmResponse getList(
      @PathVariable UUID bsId,
      @RequestParam(required = false, defaultValue = "") String search) {
    try {
      List<MoneyTrnTempl> list = moneyTrnTemplsDao.getMoneyTrnTempls(bsId, search);
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
    moneyTrnTemplsDao.createMoneyTrnTempl(bsId, templ);
    return HmResponse.getOk();
  }

  @RequestMapping("/skip")
  public HmResponse skip(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrnTempl templ) {
    templ.setNextDate(MoneyTrnTempl.calcNextDate(templ.getNextDate(), templ.getPeriod()));
    moneyTrnTemplsDao.updateMoneyTrnTempl(bsId, templ);
    return HmResponse.getOk();
  }

  @RequestMapping("/delete")
  public HmResponse delete(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrnTempl templ) {
    try {
      moneyTrnTemplsDao.deleteMoneyTrnTempl(bsId, templ.getId());
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
      moneyTrnTemplsDao.updateMoneyTrnTempl(bsId, templ);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

}
