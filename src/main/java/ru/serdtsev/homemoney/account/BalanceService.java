package ru.serdtsev.homemoney.account;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import ru.serdtsev.homemoney.account.model.AccountType;
import ru.serdtsev.homemoney.account.model.Balance;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.moneyoper.MoneyOperItemRepo;
import ru.serdtsev.homemoney.moneyoper.MoneyOperService;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceService {
  private final BalanceSheetRepository balanceSheetRepo;
  private final BalanceRepository balanceRepo;
  private final MoneyOperItemRepo moneyOperItemRepo;
  private final ReserveRepository reserveRepo;
  private final MoneyOperService moneyOperService;

  public List<Balance> getBalances(UUID bsId) {
    val balanceSheet = balanceSheetRepo.findOne(bsId);
    return ((List<Balance>) balanceRepo.findByBalanceSheet(balanceSheet)).stream()
        .filter(balance -> balance.getType() != AccountType.reserve)
        .sorted(Comparator.comparing(Balance::getNum))
        .collect(Collectors.toList());
  }

  public void createBalance(UUID bsId, UUID balanceId) {
    val balanceSheet = balanceSheetRepo.findOne(bsId);
    val balance = balanceRepo.findOne(balanceId);
    balance.setBalanceSheet(balanceSheet);
    balance.init(reserveRepo);
    balanceRepo.save(balance);
  }

  public void updateBalance(Balance balance) {
    val storedBalance = balanceRepo.findOne(balance.getId());
    storedBalance.merge(balance, reserveRepo, moneyOperService);
    balanceRepo.save(storedBalance);
  }

  public void deleteOrArchiveBalance(UUID balanceId) {
    val balance = balanceRepo.findOne(balanceId);
    val operFound = moneyOperItemRepo.findByBalance(balance).limit(1).count() > 0;
    if (operFound) {
      balance.setArc(true);
      balanceRepo.save(balance);
      log.info("{} moved to archive.", balance);
    } else {
      balanceRepo.delete(balance);
      log.info("{} deleted.", balance);
    }
  }

  public void upBalance(UUID bsId, UUID balanceId) {
    // todo работает неправильно, исправить
    BalanceSheet bs = balanceSheetRepo.findOne(bsId);
    Balance balance = balanceRepo.findOne(balanceId);

    List<Balance> balances = bs.getBalances().stream()
        .sorted((b1, b2) -> b1.getNum() < b2.getNum() ? -1 : (b1.getNum() > b2.getNum() ? 1 : 0))
        .collect(Collectors.toList());
    assert !balances.isEmpty();

    Balance prev;
    do {
      int index = balances.indexOf(balance);
      assert index > -1;
      prev = null;
      if (index > 0) {
        prev = balances.get(index - 1);
        balances.set(index - 1, balance);
        balances.set(index, prev);
        long i = 0;
        for (Balance b : balances) {
          b.setNum(i++);
        }
      }
    } while (prev != null && prev.getArc());
    balanceRepo.save(balances);
  }

}
