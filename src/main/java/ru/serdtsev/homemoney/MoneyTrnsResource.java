package ru.serdtsev.homemoney;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.serdtsev.homemoney.common.HmException;
import ru.serdtsev.homemoney.dao.MoneyTrnsDao;
import ru.serdtsev.homemoney.common.HmResponse;
import ru.serdtsev.homemoney.dto.MoneyTrn;
import ru.serdtsev.homemoney.dto.PagedList;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ru.serdtsev.homemoney.common.HmResponse.getFail;
import static ru.serdtsev.homemoney.common.HmResponse.getOk;

@RestController
@RequestMapping("/api/{bsId}/money-trns")
public class MoneyTrnsResource {
  private MoneyTrnsDao moneyTrnsDao;

  @Autowired
  public MoneyTrnsResource(MoneyTrnsDao moneyTrnsDao) {
    this.moneyTrnsDao = moneyTrnsDao;
  }

  @RequestMapping
  public HmResponse getMoneyTrns(
      @PathVariable UUID bsId,
      @RequestParam(required = false, defaultValue = "") String search,
      @RequestParam(required = false, defaultValue = "10") int limit,
      @RequestParam(required = false, defaultValue = "0") int offset) {
    try {
      ArrayList<MoneyTrn> trns = new ArrayList<>();
      if (offset == 0) {
        LocalDate beforeDate = LocalDate.now().plusDays(14L);
        List<MoneyTrn> pendingTrns = moneyTrnsDao.getPendingAndRecurrenceMoneyTrns(bsId, search, Date.valueOf(beforeDate));
        trns.addAll(pendingTrns);
      }

      List<MoneyTrn> doneTrns = moneyTrnsDao.getDoneMoneyTrns(bsId, search, limit + 1, offset);
      boolean hasNext = doneTrns.size() > limit;
      trns.addAll(hasNext ? doneTrns.subList(0, limit) : doneTrns);
      PagedList<MoneyTrn> pagedList = new PagedList<>(trns, limit, offset, hasNext);
      return getOk(pagedList);
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }

  @RequestMapping("/item")
  public HmResponse getMoneyTrn(
      @PathVariable UUID bsId,
      @RequestParam UUID id) {
    try {
      return getOk(moneyTrnsDao.getMoneyTrn(bsId, id));
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }

  @RequestMapping("/create")
  public HmResponse createMoneyTrn(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrn moneyTrn) {
    return getOk(moneyTrnsDao.createMoneyTrn(bsId, moneyTrn));
  }

  @RequestMapping("/delete")
  public HmResponse deleteMoneyTrn(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrn moneyTrn) {
    try {
      moneyTrnsDao.deleteMoneyTrn(bsId, moneyTrn.getId());
      return getOk();
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }

  @RequestMapping("/update")
  public HmResponse updateMoneyTrn(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrn moneyTrn) {
    try {
      moneyTrnsDao.updateMoneyTrn(bsId, moneyTrn);
      return getOk();
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }

  @RequestMapping("/skip")
  public HmResponse skipMoneyTrn(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrn moneyTrn) {
    try {
      moneyTrnsDao.skipMoneyTrn(bsId, moneyTrn);
      return getOk();
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }

  @RequestMapping("/up")
  public HmResponse upMoneyTrn(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrn moneyTrn) {
    try {
      moneyTrnsDao.upMoneyTrn(bsId, moneyTrn.getId());
      return getOk();
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }
}
