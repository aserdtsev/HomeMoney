package ru.serdtsev.homemoney;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serdtsev.homemoney.dao.MainDao;
import ru.serdtsev.homemoney.dto.BalanceSheet;

import java.util.UUID;

@RestController
@RequestMapping("/api/database")
public class DatabaseResource {
  @RequestMapping("/clear-n-create-balance-sheet")
  public final BalanceSheet clearDatabaseNCreateBalanceSheet() {
    MainDao.clearDatabase();
    UUID bsId = UUID.randomUUID();
    MainDao.createBalanceSheet(bsId);
    return MainDao.getBalanceSheet(bsId);
  }
}
