package ru.serdtsev.homemoney;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.account.Account;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.dto.HmResponse;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@RestController
public final class AccountsResource {
  private BalanceSheetRepository balanceSheetRepo;

  @Autowired
  public AccountsResource(BalanceSheetRepository balanceSheetRepo) {
    this.balanceSheetRepo = balanceSheetRepo;
  }

  @RequestMapping("/api/{bsId}/accounts")
  public final HmResponse getAccountList(@PathVariable UUID bsId) {
    BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
    List<Account> accounts = balanceSheet.getAccounts().stream()
        .sorted(Comparator.comparing(Account::getSortIndex))
        .collect(Collectors.toList());
    return HmResponse.getOk(accounts);
  }
}
