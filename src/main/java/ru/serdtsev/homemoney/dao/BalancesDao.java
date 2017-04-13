package ru.serdtsev.homemoney.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;

@Component
public class BalancesDao {
  private AccountsDao accountsDao;
  private BalanceSheetRepository balanceSheetRepo;

  @Autowired
  public BalancesDao(AccountsDao accountsDao, BalanceSheetRepository balanceSheetRepo) {
    this.accountsDao = accountsDao;
    this.balanceSheetRepo = balanceSheetRepo;
  }
}
