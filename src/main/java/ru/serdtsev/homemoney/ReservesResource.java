package ru.serdtsev.homemoney;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.account.BalanceService;
import ru.serdtsev.homemoney.account.model.AccountType;
import ru.serdtsev.homemoney.account.model.Reserve;
import ru.serdtsev.homemoney.account.ReserveRepository;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.common.HmException;
import ru.serdtsev.homemoney.common.HmResponse;
import ru.serdtsev.homemoney.moneyoper.MoneyOperService;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/{bsId}/reserves")
@RequiredArgsConstructor
public class ReservesResource {
  private final ReserveRepository reserveRepo;
  private final BalanceSheetRepository balanceSheetRepo;
  private final MoneyOperService moneyOperService;
  private final BalanceService balanceService;

  @RequestMapping
  public HmResponse getReserveList(@PathVariable UUID bsId) {
    BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
    List<Reserve> reserves = ((List<Reserve>) reserveRepo.findByBalanceSheet(balanceSheet)).stream()
        .sorted(Comparator.comparing(Reserve::getCreated))
        .collect(Collectors.toList());
    return HmResponse.getOk(reserves);
  }

  @RequestMapping("/create")
  @Transactional(readOnly = true)
  public HmResponse createReserve(
      @PathVariable UUID bsId,
      @RequestBody Reserve reserve) {
    try {
      BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
      reserve.setBalanceSheet(balanceSheet);
      reserve.setType(AccountType.reserve);
      reserve.setCurrencyCode(balanceSheet.getCurrencyCode());
      reserve.setCreated(java.sql.Date.valueOf(LocalDate.now()));
      reserveRepo.save(reserve);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/update")
  @Transactional
  public HmResponse updateReserve(
      @PathVariable UUID bsId,
      @RequestBody Reserve reserve) {
    try {
      Reserve currReserve = reserveRepo.findOne(reserve.getId());
      currReserve.merge(reserve, reserveRepo, moneyOperService);
      reserveRepo.save(currReserve);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/delete")
  @Transactional
  public HmResponse deleteOrArchiveReserve(
      @PathVariable UUID bsId,
      @RequestBody Reserve reserve) {
    try {
      balanceService.deleteOrArchiveBalance(reserve.getId());
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }
}
