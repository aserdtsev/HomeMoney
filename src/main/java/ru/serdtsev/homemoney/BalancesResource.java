package ru.serdtsev.homemoney;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.dao.BalancesDao;
import ru.serdtsev.homemoney.dao.MoneyTrnsDao;
import ru.serdtsev.homemoney.dto.Balance;
import ru.serdtsev.homemoney.dto.HmResponse;
import ru.serdtsev.homemoney.dto.MoneyTrn;

import java.util.UUID;

@RestController
@RequestMapping("/api/{bsId}/balances")
public final class BalancesResource {
  private BalancesDao balancesDao;
  private MoneyTrnsDao moneyTrnsDao;

  @Autowired
  public BalancesResource(BalancesDao balancesDao, MoneyTrnsDao moneyTrnsDao) {
    this.balancesDao = balancesDao;
    this.moneyTrnsDao = moneyTrnsDao;
  }

  @RequestMapping
  public final HmResponse getBalances(@PathVariable UUID bsId) {
    return HmResponse.getOk(balancesDao.getBalances(bsId));
  }

  @RequestMapping("/create")
  public final HmResponse createBalance(
      @PathVariable UUID bsId,
      @RequestBody Balance balance) {
    try {
      balancesDao.createBalance(bsId, balance);
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
      MoneyTrn moneyTrn = balancesDao.updateBalance(bsId, balance);
      moneyTrnsDao.createMoneyTrn(bsId, moneyTrn);
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
      balancesDao.deleteBalance(bsId, balance.getId());
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
      balancesDao.upBalance(bsId, balance);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }
}
