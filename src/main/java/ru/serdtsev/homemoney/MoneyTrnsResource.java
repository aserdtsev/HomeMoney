package ru.serdtsev.homemoney;

import org.springframework.web.bind.annotation.*;
import ru.serdtsev.homemoney.dao.MoneyTrnsDao;
import ru.serdtsev.homemoney.dto.HmResponse;
import ru.serdtsev.homemoney.dto.MoneyTrn;
import ru.serdtsev.homemoney.dto.PagedList;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ru.serdtsev.homemoney.dto.HmResponse.getFail;
import static ru.serdtsev.homemoney.dto.HmResponse.getOk;

@RestController
@RequestMapping("/api/{bsId}/money-trns")
public class MoneyTrnsResource {
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
        List<MoneyTrn> pendingTrns = MoneyTrnsDao.INSTANCE.getPendingMoneyTrns(bsId, search, Date.valueOf(beforeDate));
        trns.addAll(pendingTrns);
      }

      List<MoneyTrn> doneTrns = MoneyTrnsDao.INSTANCE.getDoneMoneyTrns(bsId, search, limit + 1, offset);
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
      return getOk(MoneyTrnsDao.INSTANCE.getMoneyTrn(bsId, id));
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }

  @RequestMapping("/create")
  public HmResponse createMoneyTrn(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrn moneyTrn) {
    return getOk(MoneyTrnsDao.INSTANCE.createMoneyTrn(bsId, moneyTrn));
  }

  @RequestMapping("/delete")
  public HmResponse deleteMoneyTrn(
      @PathVariable UUID bsId,
      @RequestBody MoneyTrn moneyTrn) {
    try {
      MoneyTrnsDao.INSTANCE.deleteMoneyTrn(bsId, moneyTrn.getId());
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
      MoneyTrnsDao.INSTANCE.updateMoneyTrn(bsId, moneyTrn);
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
      MoneyTrnsDao.INSTANCE.skipMoneyTrn(bsId, moneyTrn);
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
      MoneyTrnsDao.INSTANCE.upMoneyTrn(bsId, moneyTrn.getId());
      return getOk();
    } catch (HmException e) {
      return getFail(e.getCode());
    }
  }
}
