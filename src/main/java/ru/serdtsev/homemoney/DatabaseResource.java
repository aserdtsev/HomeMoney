package ru.serdtsev.homemoney;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.dao.MainDao;

@RestController
@RequestMapping("/api/database")
public class DatabaseResource {
  private final MainDao mainDao;

  @Autowired
  public DatabaseResource(MainDao mainDao) {
    this.mainDao = mainDao;
  }

  @RequestMapping("/clear-n-create-balance-sheet")
  @Transactional
  public BalanceSheet clearDatabaseNCreateBalanceSheet() {
    mainDao.clearDatabase();
    return BalanceSheet.Companion.newInstance().init();
  }
}
