package ru.serdtsev.homemoney;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.dao.MainDao;

@RestController
@RequestMapping("/api/database")
public class DatabaseResource {
  @RequestMapping("/clear-n-create-balance-sheet")
  @Transactional
  public final BalanceSheet clearDatabaseNCreateBalanceSheet() {
    MainDao.clearDatabase();
    return BalanceSheet.Companion.newInstance().init();
  }
}
