package ru.serdtsev.homemoney;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.account.AccountType;
import ru.serdtsev.homemoney.account.Reserve;
import ru.serdtsev.homemoney.account.ReserveRepository;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.dao.AccountsDao;
import ru.serdtsev.homemoney.dao.MoneyTrnTemplsDao;
import ru.serdtsev.homemoney.dao.MoneyTrnsDao;
import ru.serdtsev.homemoney.dao.ReservesDao;
import ru.serdtsev.homemoney.dto.HmResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/{bsId}/reserves")
public final class ReservesResource {
  private ReserveRepository reserveRepo;
  private BalanceSheetRepository balanceSheetRepo;
  private ReservesDao reservesDao;
  private MoneyTrnsDao moneyTrnsDao;

  @Autowired
  public ReservesResource(ReserveRepository reserveRepo, BalanceSheetRepository balanceSheetRepo, ReservesDao reservesDao,
      MoneyTrnsDao moneyTrnsDao) {
    this.reserveRepo = reserveRepo;
    this.balanceSheetRepo = balanceSheetRepo;
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
      BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
      reserve.setBalanceSheet(balanceSheet);
      reserve.setType(AccountType.reserve);
      reserveRepo.save(reserve);
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
      Reserve currReserve = reserveRepo.findOne(reserve.getId());
      currReserve.merge(reserve);
      reserveRepo.save(reserve);
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
      if (!AccountsDao.isTrnExists(reserve.getId()) && !MoneyTrnTemplsDao.isTrnTemplExists(reserve.getId())) {
        reserveRepo.delete(reserve.getId());
      }
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }
}
