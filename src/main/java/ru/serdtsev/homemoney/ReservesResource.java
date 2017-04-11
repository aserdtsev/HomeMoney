package ru.serdtsev.homemoney;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.account.AccountType;
import ru.serdtsev.homemoney.dao.MoneyTrnsDao;
import ru.serdtsev.homemoney.dao.ReservesDao;
import ru.serdtsev.homemoney.dto.HmResponse;
import ru.serdtsev.homemoney.dto.MoneyTrn;
import ru.serdtsev.homemoney.dto.Reserve;

import java.util.UUID;

@RestController
@RequestMapping("/api/{bsId}/reserves")
public final class ReservesResource {
  private ReservesDao reservesDao;
  private MoneyTrnsDao moneyTrnsDao;

  @Autowired
  public ReservesResource(ReservesDao reservesDao, MoneyTrnsDao moneyTrnsDao) {
    this.reservesDao = reservesDao;
    this.moneyTrnsDao = moneyTrnsDao;
  }

  @RequestMapping
  public final HmResponse getReserveList(@PathVariable UUID bsId) {
    return HmResponse.getOk(ReservesDao.getReserves(bsId));
  }

  @RequestMapping("/create")
  public final HmResponse createReserve(
      @PathVariable UUID bsId,
      @RequestBody Reserve reserve) {
    try {
      reserve.setType(AccountType.reserve);
      reservesDao.createReserve(bsId, reserve);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/update")
  public final HmResponse updateReserve(
      @PathVariable UUID bsId,
      @RequestBody Reserve reserve) {
    try {
      MoneyTrn moneyTrn = reservesDao.updateReserve(bsId, reserve);
      moneyTrnsDao.createMoneyTrn(bsId, moneyTrn);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/delete")
  public final HmResponse deleteReserve(
      @PathVariable UUID bsId,
      @RequestBody Reserve reserve) {
    try {
      reservesDao.deleteReserve(bsId, reserve.getId());
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }
}
