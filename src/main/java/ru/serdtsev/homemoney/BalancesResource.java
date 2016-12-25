package ru.serdtsev.homemoney;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.dao.BalancesDao;
import ru.serdtsev.homemoney.dto.Balance;
import ru.serdtsev.homemoney.dto.HmResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/{bsId}/balances")
public final class BalancesResource {
  @RequestMapping
  public final HmResponse getBalances(@PathVariable UUID bsId) {
    return HmResponse.getOk(BalancesDao.getBalances(bsId));
  }

  @RequestMapping("/create")
  public final HmResponse createBalance(
      @PathVariable UUID bsId,
      @RequestBody Balance balance) {
    try {
      BalancesDao.createBalance(bsId, balance);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/update")
  public final HmResponse updateBalance(
      @PathVariable UUID bsId,
      @RequestBody Balance balance) {
    try {
      BalancesDao.updateBalance(bsId, balance);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/delete")
  public final HmResponse deleteBalance(
      @PathVariable UUID bsId,
      @RequestBody Balance balance) {
    try {
      BalancesDao.deleteBalance(bsId, balance.getId());
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/up")
  public final HmResponse upBalance(
      @PathVariable UUID bsId,
      @RequestBody Balance balance) {
    try {
      BalancesDao.upBalance(bsId, balance);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }
}
