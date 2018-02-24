package ru.serdtsev.homemoney;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.account.BalanceService;
import ru.serdtsev.homemoney.account.model.Balance;
import ru.serdtsev.homemoney.common.HmException;
import ru.serdtsev.homemoney.common.HmResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/{bsId}/balances")
@RequiredArgsConstructor
public class BalancesResource {
  private final BalanceService balanceService;

  @RequestMapping
  @Transactional(readOnly = true)
  public HmResponse getBalances(@PathVariable UUID bsId) {
    List<Balance> balances = balanceService.getBalances(bsId);
    return HmResponse.getOk(balances);
  }

  @RequestMapping("/create")
  @Transactional
  public HmResponse createBalance(
      @PathVariable UUID bsId,
      @RequestBody Balance balance) {
    try {
      balanceService.createBalance(bsId, balance.getId());
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/update")
  @Transactional
  public HmResponse updateBalance(
      @PathVariable UUID bsId,
      // todo Заменить BalanceDto на Balance.
      @RequestBody Balance balance) {
    try {
      balanceService.updateBalance(balance);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/delete")
  @Transactional
  public HmResponse deleteOrArchiveBalance(
      @PathVariable UUID bsId,
      @RequestBody Balance balance) {
    try {
      balanceService.deleteOrArchiveBalance(balance.getId());
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/up")
  @Transactional
  public HmResponse upBalance(
      @PathVariable UUID bsId,
      @RequestBody Balance balance) {
    try {
      balanceService.upBalance(bsId, balance.getId());
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }
}
