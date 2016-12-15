package ru.serdtsev.homemoney

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.serdtsev.homemoney.dao.MainDao
import ru.serdtsev.homemoney.dto.BalanceSheet
import java.util.*

@RestController
@RequestMapping("/api/database")
class DatabaseResource {
  @RequestMapping("/clear-n-create-balance-sheet")
  fun clearDatabaseNCreateBalanceSheet(): BalanceSheet {
    MainDao.clearDatabase()
    MainDao.createBalanceSheet(UUID.randomUUID())
    return MainDao.balanceSheets[0]
  }
}
