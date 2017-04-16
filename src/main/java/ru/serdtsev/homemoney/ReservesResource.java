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
import ru.serdtsev.homemoney.account.AccountsDao;
import ru.serdtsev.homemoney.dao.MoneyTrnTemplsDao;
import ru.serdtsev.homemoney.dto.HmResponse;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/{bsId}/reserves")
public final class ReservesResource {
  private ReserveRepository reserveRepo;
  private BalanceSheetRepository balanceSheetRepo;

  @Autowired
  public ReservesResource(ReserveRepository reserveRepo, BalanceSheetRepository balanceSheetRepo) {
    this.reserveRepo = reserveRepo;
    this.balanceSheetRepo = balanceSheetRepo;
  }

  @RequestMapping
  public final HmResponse getReserveList(@PathVariable UUID bsId) {
    BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
    List<Reserve> reserves = ((List<Reserve>) reserveRepo.findByBalanceSheet(balanceSheet)).stream()
        .sorted(Comparator.comparing(Reserve::getCreated))
        .collect(Collectors.toList());
    return HmResponse.getOk(reserves);
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
