package ru.serdtsev.homemoney;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.account.*;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.dao.AccountsDao;
import ru.serdtsev.homemoney.dao.MoneyTrnTemplsDao;
import ru.serdtsev.homemoney.dao.MoneyTrnsDao;
import ru.serdtsev.homemoney.dto.HmResponse;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/{bsId}/balances")
public final class BalancesResource {
  private BalanceSheetRepository balanceSheetRepo;
  private BalanceRepository balanceRepo;
  private AccountRepository accountRepo;
  private ReserveRepository reserveRepo;
  private MoneyTrnsDao moneyTrnsDao;

  @Autowired
  public BalancesResource(BalanceSheetRepository balanceSheetRepo, BalanceRepository balanceRepo,
      AccountRepository accountRepo, ReserveRepository reserveRepo, MoneyTrnsDao moneyTrnsDao) {
    this.balanceSheetRepo = balanceSheetRepo;
    this.balanceRepo = balanceRepo;
    this.accountRepo = accountRepo;
    this.moneyTrnsDao = moneyTrnsDao;
    this.reserveRepo = reserveRepo;
  }

  @RequestMapping
  public final HmResponse getBalances(@PathVariable UUID bsId) {
    BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
    List<Balance> balances = ((List<Balance>) balanceRepo.findByBalanceSheet(balanceSheet)).stream()
        .filter(balance -> balance.getType() != AccountType.reserve)
        .sorted(Comparator.comparing(Balance::getNum))
        .collect(Collectors.toList());
    return HmResponse.getOk(balances);
  }

  @RequestMapping("/create")
  public final HmResponse createBalance(
      @PathVariable UUID bsId,
      @RequestBody Balance balance) {
    try {
      BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
      balance.setBalanceSheet(balanceSheet);
      balance.init();
      balanceRepo.save(balance);
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/update")
  public final HmResponse updateBalance(
      @PathVariable UUID bsId,
      // todo Заменить BalanceDto на Balance.
      @RequestBody Balance balance) {
    try {
      Balance currBalance = balanceRepo.findOne(balance.getId());
      currBalance.merge(balance, reserveRepo, moneyTrnsDao);
      balanceRepo.save(currBalance);
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
      Balance currBalance = balanceRepo.findOne(balance.getId());
      if (!AccountsDao.isTrnExists(currBalance.getId()) && !MoneyTrnTemplsDao.isTrnTemplExists(currBalance.getId())) {
        balanceRepo.delete(currBalance);
      }
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }

  @RequestMapping("/up")
  public final HmResponse upBalance(
      @PathVariable UUID bsId,
      @RequestBody Balance balance) {
    // todo работает неправильно, исправить
    try {
      Balance currBalance = balanceRepo.findOne(balance.getId());
      BalanceSheet bs = currBalance.getBalanceSheet();

      List<Balance> balances = bs.getBalances().stream()
          .sorted((b1, b2) -> b1.getNum() < b2.getNum() ? -1 : (b1.getNum() > b2.getNum() ? 1 : 0))
          .collect(Collectors.toList());
      assert !balances.isEmpty();

      Balance prev;
      do {
        int index = balances.indexOf(currBalance);
        assert index > -1;
        prev = null;
        if (index > 0) {
          prev = balances.get(index - 1);
          balances.set(index - 1, currBalance);
          balances.set(index, prev);
          long i = 0;
          for (Balance b : balances) {
            b.setNum(i++);
          }
        }
      } while (prev != null && prev.getArc());
      balances.forEach(b -> balanceRepo.save(b));
      return HmResponse.getOk();
    } catch (HmException e) {
      return HmResponse.getFail(e.getCode());
    }
  }
}
